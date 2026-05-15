package com.realengine.offlineuniplugin.retool;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;

import org.json.JSONArray;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ZIP 压缩/解压工具类（静态工具版）
 * 功能：
 * 1. 支持加密/无密码解压（本地文件 + SAF 流）
 * 2. 自动剥离 ZIP 顶层公共文件夹（本地文件版本）
 * 3. 读取 ZIP 全局注释 + 文件注释
 * 4. 路径安全校验，防止目录穿越
 * 5. 支持异步解压 + 进度回调
 * 6. 全部静态方法，无需实例化
 *
 * @author RealEngine
 */
public class ZipUtil {
    private static final String TAG = "ZipUtil";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * 解压进度回调接口（所有回调均在主线程执行）
     */
    public interface UnzipProgressCallback {
        /** 解压开始，totalEntries 为 ZIP 内总条目数（可能为 -1 表示未知） */
        void onStart(int totalEntries);

        /** 进度更新，processed 当前已处理条目数（含目录），entryName 当前条目名称 */
        void onProgress(int processed, int totalEntries, String entryName);

        /** 单个文件解压成功（可选，用于更细粒度提示） */
        void onFileSuccess(String entryName);

        /** 解压完成，destPath 为解压目录的绝对路径（本地版本）或 URI（SAF 版本） */
        void onSuccess(String destPath);

        /** 解压失败 */
        void onFailure(Exception e);
    }

    /**
     * 适配器类，简化回调实现，只需重写 onProgress 和 onSuccess
     */
    public static abstract class SimpleUnzipProgressCallback implements UnzipProgressCallback {
        @Override public void onStart(int totalEntries) {}
        @Override public void onFileSuccess(String entryName) {}
        @Override public void onFailure(Exception e) {}
        // 这两个仍然保持 abstract，强制调用方实现
        @Override public abstract void onProgress(int processed, int totalEntries, String entryName);
        @Override public abstract void onSuccess(String destPath);
    }

    private ZipUtil() {
    }

    // ============================== 对外静态方法（本地文件版本） ==============================

    /**
     * 同步解压本地 ZIP 文件（支持加密）- 必须在子线程调用
     *
     * @param filePath ZIP 文件绝对路径
     * @param password 解压密码，无密码传 null
     * @return 解压成功返回解压目录绝对路径，失败返回空字符串
     */
    @WorkerThread
    public static String unzipFile(String filePath, String password) {
        return unzipFileInternal(filePath, password, null, -1);
    }

    /**
     * 异步解压本地 ZIP 文件，支持进度回调，自动在后台线程执行，回调在主线程。
     *
     * @param filePath ZIP 文件绝对路径
     * @param password 解压密码，无密码传 null
     * @param callback 进度回调（可为 null）
     */
    public static void unzipFileAsync(String filePath, String password, @Nullable UnzipProgressCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                int totalEntries = -1;
                if (callback != null) {
                    totalEntries = countZipEntriesLocal(filePath, password);
                    final int finalTotal = totalEntries;
                    MAIN_HANDLER.post(() -> callback.onStart(finalTotal));
                }
                String result = unzipFileInternal(filePath, password, callback, totalEntries);
                if (callback != null) {
                    final String finalResult = result;
                    MAIN_HANDLER.post(() -> {
                        if (finalResult != null && !finalResult.isEmpty()) {
                            callback.onSuccess(finalResult);
                        } else {
                            callback.onFailure(new Exception("解压失败，返回路径为空"));
                        }
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    final Exception finalException = e;
                    MAIN_HANDLER.post(() -> callback.onFailure(finalException));
                }
            }
        });
    }



    // ============================== 对外静态方法（SAF 流版本） ==============================

    /**
     * 同步解压 ZIP 文件（SAF 版本） - 必须在子线程调用
     *
     * @param filePath ZIP 文件的 SAF 路径字符串
     * @param password 解压密码，无密码传 null
     * @param context  上下文
     * @return 解压后文件夹的 SAF URI 字符串，失败返回 null
     */
    @WorkerThread
    public static String unzipFileFromStream(String filePath, String password, Context context) {
        return unzipFileFromStreamInternal(filePath, password, context, null, -1);
    }

    /**
     * 异步解压 ZIP 文件（SAF 版本），支持进度回调，自动在后台线程执行，回调在主线程。
     *
     * @param filePath ZIP 文件的 SAF 路径
     * @param password 解压密码（无密码传 null）
     * @param context  上下文
     * @param callback 进度回调（可为 null）
     */
    public static void unzipFileFromStreamAsync(String filePath, String password, Context context,
                                                @Nullable UnzipProgressCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                int totalEntries = -1;
                if (callback != null) {
                    totalEntries = countZipEntriesFromStream(filePath, password, context);
                    final int finalTotal = totalEntries;
                    MAIN_HANDLER.post(() -> callback.onStart(finalTotal));
                }
                String result = unzipFileFromStreamInternal(filePath, password, context, callback, totalEntries);
                if (callback != null) {
                    final String finalResult = result;
                    MAIN_HANDLER.post(() -> {
                        if (finalResult != null) {
                            callback.onSuccess(finalResult);
                        } else {
                            callback.onFailure(new Exception("解压失败，返回结果为 null"));
                        }
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    final Exception finalException = e;
                    MAIN_HANDLER.post(() -> callback.onFailure(finalException));
                }
            }
        });
    }



    // ============================== 注释读取方法 ==============================

    /**
     * 仅读取ZIP注释信息（不解压/支持加密）- 本地文件版本
     */
    public static JSONObject readZipCommentsOnly(String filePath) {
        JSONObject result = new JSONObject();
        if (!FileUtil.fileExist(filePath)) {
            result.put("error", "ZIP文件不存在");
            return result;
        }

        File zipFile = new File(filePath);
        try (ZipFile zip = new ZipFile(zipFile)) {
            String globalComment = zip.getComment();
            if (globalComment != null && !globalComment.isEmpty()) {
                try {
                    JSONObject globalObj = JSON.parseObject(globalComment);
                    if (globalObj != null && !globalObj.isEmpty()) {
                        result.put("globalComment", globalObj);
                    } else {
                        result.put("globalComment", globalComment);
                    }
                } catch (JSONException e) {
                    result.put("globalComment", globalComment);
                }
            } else {
                result.put("globalComment", "");
            }

            JSONArray fileComments = new JSONArray();
            List<FileHeader> headers = zip.getFileHeaders();
            for (FileHeader header : headers) {
                String fileComment = header.getFileComment();
                if (fileComment != null && !fileComment.isEmpty()) {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("fileName", header.getFileName());
                    fileObj.put("comment", fileComment);
                    fileComments.put(fileObj);
                }
            }
            result.put("fileComments", fileComments);
            return result;
        } catch (ZipException e) {
            Log.e(TAG, "读取ZIP注释失败", e);
            result.put("error", "读取失败：" + e.getMessage());
            return result;
        } catch (IOException e) {
            Log.e(TAG, "IO异常", e);
            result.put("error", "读取失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 仅读取ZIP注释信息（不解压/支持加密）（SAF 流版本）
     * 【纯手动解析ZIP协议，不依赖解压库，支持超大文件，仅读末尾数据】
     */
    public static JSONObject readZipCommentsOnlyFromStream(String filePath, Context context) {
        JSONObject result = new JSONObject();
        try {
            Uri uri = Uri.parse(filePath);
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
            if (documentFile == null) {
                throw new Exception("无效的SAF文件");
            }
            long fileLength = documentFile.length();

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                final int MAX_COMMENT_LEN = 65535;
                final int TAIL_READ_SIZE = MAX_COMMENT_LEN + 22;

                long skipTotal = Math.max(0, fileLength - TAIL_READ_SIZE);
                long skipped;
                while (skipTotal > 0 && (skipped = inputStream.skip(skipTotal)) > 0) {
                    skipTotal -= skipped;
                }

                byte[] tailBuffer = new byte[TAIL_READ_SIZE];
                int readBytes = inputStream.read(tailBuffer);

                for (int i = readBytes - 4; i >= 0; i--) {
                    if (tailBuffer[i] == 0x50 && tailBuffer[i+1] == 0x4B && tailBuffer[i+2] == 0x05 && tailBuffer[i+3] == 0x06) {
                        int commentLength = ((tailBuffer[i+21] & 0xFF) << 8) | (tailBuffer[i+20] & 0xFF);
                        if (commentLength <= 0) {
                            break;
                        }
                        int commentStartIndex = i + 22;
                        byte[] commentBytes = new byte[commentLength];
                        System.arraycopy(tailBuffer, commentStartIndex, commentBytes, 0, commentLength);
                        String globalComment = new String(commentBytes, StandardCharsets.UTF_8);
                        JSONObject globalObj = JSON.parseObject(globalComment);
                        result.put("globalComment", globalObj);
                        break;
                    }
                }
                result.put("fileComments", new JSONArray());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "SAF解析失败：" + e.getMessage());
            return result;
        }
    }



    // ============================== 内部实现（本地文件版本） ==============================

    /**
     * 内部同步解压本地 ZIP 文件（支持进度回调）
     *
     * @param totalEntries 总条目数（-1 表示未知）
     */
    @WorkerThread
    private static String unzipFileInternal(String filePath, String password,
                                            @Nullable UnzipProgressCallback callback,
                                            int totalEntries) {
        Log.d(TAG, "开始解压本地文件 → " + filePath + (password != null ? " (加密)" : ""));

        if (!FileUtil.fileExist(filePath)) {
            Log.e(TAG, "ZIP文件不存在：" + filePath);
            return "";
        }

        File zipFile = new File(filePath);
        File parentDir = zipFile.getParentFile();
        String zipFileName = zipFile.getName();
        String unzipDirName = zipFileName.substring(0, zipFileName.toLowerCase().lastIndexOf(".zip"));
        File unzipRootDir = new File(parentDir, unzipDirName);

        if (!unzipRootDir.exists() && !unzipRootDir.mkdirs()) {
            Log.e(TAG, "创建解压根目录失败：" + unzipRootDir.getAbsolutePath());
            return "";
        }

        // 读取注释（仅日志，不影响解压）
        readZipComments(zipFile, password);

        // 智能剥离顶层文件夹
        String stripPrefix = findStripPrefixWithZip4j(zipFile, unzipDirName, password);
        if (stripPrefix != null) {
            Log.d(TAG, "检测到公共顶层文件夹，将剥离前缀: " + stripPrefix);
        }

        // 执行解压（带进度）
        return unzipWithZip4jInternal(zipFile, unzipRootDir, stripPrefix, password, callback, totalEntries);
    }

    /**
     * 统计本地 ZIP 文件总条目数（用于进度）
     */
    private static int countZipEntriesLocal(String filePath, String password) {
        try (ZipFile zip = new ZipFile(new File(filePath))) {
            if (password != null && !password.isEmpty()) {
                zip.setPassword(password.toCharArray());
            }
            return zip.getFileHeaders().size();
        } catch (IOException e) {
            Log.e(TAG, "预扫描本地 ZIP 条目失败", e);
            return -1;
        }
    }

    /**
     * 内部解压核心（本地文件，支持进度回调）
     */
    private static String unzipWithZip4jInternal(File zipFile, File unzipRootDir, String stripPrefix,
                                                 String password, @Nullable UnzipProgressCallback callback,
                                                 int totalEntries) {
        int entryCount = 0;
        int successCount = 0;

        try (ZipFile zip = new ZipFile(zipFile)) {
            if (password != null && !password.isEmpty()) {
                zip.setPassword(password.toCharArray());
            }

            List<FileHeader> fileHeaders = zip.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                entryCount++;
                String entryName = header.getFileName();
                Log.d(TAG, "正在处理第 " + entryCount + " 项: " + entryName);

                // 进度回调
                if (callback != null) {
                    final int current = entryCount;
                    final String name = entryName;
                    MAIN_HANDLER.post(() -> callback.onProgress(current, totalEntries, name));
                }

                // 剥离顶层目录
                String relativePath = entryName;
                if (stripPrefix != null) {
                    if (entryName.startsWith(stripPrefix)) {
                        relativePath = entryName.substring(stripPrefix.length());
                    } else if (entryName.equals(stripPrefix.substring(0, stripPrefix.length() - 1))) {
                        Log.d(TAG, "跳过顶层目录: " + entryName);
                        continue;
                    }
                }

                if (relativePath.isEmpty()) {
                    Log.d(TAG, "跳过空路径条目: " + entryName);
                    continue;
                }

                // 处理目录
                if (header.isDirectory()) {
                    File dir = new File(unzipRootDir, relativePath);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Log.e(TAG, "创建目录失败: " + dir.getAbsolutePath());
                    }
                    continue;
                }

                // 处理文件
                File targetFile = new File(unzipRootDir, relativePath);
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    Log.e(TAG, "创建父目录失败: " + parentDir.getAbsolutePath());
                    continue;
                }

                if (!isValidTargetFile(targetFile, unzipRootDir)) {
                    Log.e(TAG, "非法路径，已跳过：" + entryName);
                    continue;
                }

                try {
                    zip.extractFile(header, unzipRootDir.getAbsolutePath(), relativePath);
                    Log.d(TAG, "解压成功: " + targetFile.getAbsolutePath());
                    successCount++;
                    if (callback != null) {
                        final String successName = entryName;
                        MAIN_HANDLER.post(() -> callback.onFileSuccess(successName));
                    }
                } catch (ZipException e) {
                    Log.e(TAG, "解压文件失败: " + entryName, e);
                }
            }

            Log.d(TAG, "解压完成 | 总条目数：" + entryCount + " | 成功解压文件数：" + successCount);
            Log.d(TAG, "解压目录：" + unzipRootDir.getAbsolutePath());
            return unzipRootDir.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "解压异常", e);
            return "";
        }
    }

    // ============================== 内部实现（SAF 流版本） ==============================

    /**
     * 统计 SAF ZIP 文件总条目数（用于进度）
     */
    private static int countZipEntriesFromStream(String filePath, String password, Context context) {
        try {
            DocumentFile zipFile = SafUtil.getDocumentFile(filePath, context);
            if (zipFile == null || !zipFile.isFile()) return -1;
            try (InputStream rawIs = context.getContentResolver().openInputStream(zipFile.getUri())) {
                if (rawIs == null) return -1;
                try (ZipInputStream zis = new ZipInputStream(rawIs, password != null ? password.toCharArray() : null)) {
                    int count = 0;
                    while (zis.getNextEntry() != null) {
                        count++;
                    }
                    return count;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "预扫描 SAF ZIP 条目失败", e);
            return -1;
        }
    }

    /**
     * 内部同步解压 SAF ZIP 文件（支持进度回调）
     */
    @WorkerThread
    private static String unzipFileFromStreamInternal(String filePath, String password, Context context,
                                                      @Nullable UnzipProgressCallback callback,
                                                      int totalEntries) {
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "文件路径为空");
            return null;
        }

        try {
            DocumentFile zipFile = SafUtil.getDocumentFile(filePath, context);
            if (zipFile == null || !zipFile.isFile()) {
                Log.e(TAG, "ZIP文件不存在或无效：" + filePath);
                return null;
            }

            DocumentFile parentDir = SafUtil.getParentFile(filePath, context);
            if (parentDir == null) {
                Log.e(TAG, "无法获取父目录：" + filePath);
                return null;
            }

            String zipFileName = zipFile.getName();
            if (zipFileName == null || !zipFileName.toLowerCase().endsWith(".zip")) {
                Log.e(TAG, "ZIP文件名无效或不是 .zip 结尾：" + zipFileName);
                return null;
            }
            String destFolderName = zipFileName.substring(0, zipFileName.length() - 4);
            DocumentFile destFolder = parentDir.findFile(destFolderName);
            if (destFolder == null) {
                destFolder = parentDir.createDirectory(destFolderName);
                Log.d(TAG, "创建解压根目录：" + destFolderName);
            } else if (!destFolder.isDirectory()) {
                destFolder = parentDir.createDirectory(destFolderName + "_unzipped");
                Log.d(TAG, "已存在非目录同名项，使用新名称：" + destFolderName + "_unzipped");
            } else {
                Log.d(TAG, "解压根目录已存在：" + destFolderName);
            }
            if (destFolder == null) {
                Log.e(TAG, "创建解压根目录失败");
                return null;
            }

            ContentResolver resolver = context.getContentResolver();
            int entryCount = 0;
            int successFileCount = 0;

            try (InputStream rawIs = resolver.openInputStream(zipFile.getUri())) {
                if (rawIs == null) {
                    Log.e(TAG, "无法打开ZIP输入流");
                    return null;
                }
                try (ZipInputStream zis = new ZipInputStream(rawIs, password != null ? password.toCharArray() : null)) {
                    LocalFileHeader header;
                    while ((header = zis.getNextEntry()) != null) {
                        entryCount++;
                        String rawName = header.getFileName();
                        if (rawName == null || rawName.isEmpty()) {
                            Log.w(TAG, "第 " + entryCount + " 项名称为空，已跳过");
                            continue;
                        }

                        String entryName = rawName.replace('\\', '/');
                        Log.d(TAG, "正在处理第 " + entryCount + " 项: " + entryName);

                        if (callback != null) {
                            final int current = entryCount;
                            final String name = entryName;
                            MAIN_HANDLER.post(() -> callback.onProgress(current, totalEntries, name));
                        }

                        // 目录
                        if (entryName.endsWith("/")) {
                            String dirPath = entryName.substring(0, entryName.length() - 1);
                            DocumentFile createdDir = SafUtil.createSafDirectories(destFolder, dirPath);
                            if (createdDir == null) {
                                Log.e(TAG, "创建目录失败: " + dirPath);
                            } else {
                                Log.d(TAG, "创建目录成功: " + dirPath);
                            }
                            continue;
                        }

                        // 文件
                        String parentPath = "";
                        String fileName = entryName;
                        int lastSlash = entryName.lastIndexOf('/');
                        if (lastSlash > 0) {
                            parentPath = entryName.substring(0, lastSlash);
                            fileName = entryName.substring(lastSlash + 1);
                        }

                        DocumentFile targetParent = destFolder;
                        if (!parentPath.isEmpty()) {
                            targetParent = SafUtil.createSafDirectories(destFolder, parentPath);
                            if (targetParent == null) {
                                Log.e(TAG, "创建父目录失败: " + parentPath + "，跳过文件 " + entryName);
                                continue;
                            }
                        }

                        String mime = SafUtil.getMimeType(fileName);
                        DocumentFile newFile = targetParent.createFile(mime, fileName);
                        if (newFile == null) {
                            Log.e(TAG, "创建文件失败: " + fileName + " 在目录 " + targetParent.getUri());
                            continue;
                        }

                        try (OutputStream os = resolver.openOutputStream(newFile.getUri())) {
                            if (os == null) {
                                Log.w(TAG, "无法打开输出流: " + newFile.getUri());
                                continue;
                            }
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                            Log.d(TAG, "解压成功: " + entryName);
                            successFileCount++;
                            if (callback != null) {
                                final String successName = entryName;
                                MAIN_HANDLER.post(() -> callback.onFileSuccess(successName));
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "写入文件出错: " + entryName, e);
                        }
                    }
                }
            }

            Log.d(TAG, "解压完成 | 总条目数：" + entryCount + " | 成功解压文件数：" + successFileCount);
            Log.d(TAG, "解压目录 URI：" + destFolder.getUri().toString());
            return destFolder.getUri().toString();

        } catch (Exception e) {
            Log.e(TAG, "解压过程中发生异常", e);
            return null;
        }
    }



    // ============================== 辅助方法 ==============================

    private static void readZipComments(File zipFile, String password) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            if (password != null && !password.isEmpty()) {
                zip.setPassword(password.toCharArray());
            }
            String globalComment = zip.getComment();
            if (globalComment != null && !globalComment.isEmpty()) {
                Log.d(TAG, "========== ZIP全局注释 ==========");
                Log.d(TAG, globalComment);
                Log.d(TAG, "==================================");
            } else {
                Log.d(TAG, "该ZIP文件没有全局注释");
            }

            List<FileHeader> fileHeaders = zip.getFileHeaders();
            boolean hasFileComments = false;
            for (FileHeader header : fileHeaders) {
                String fileComment = header.getFileComment();
                if (fileComment != null && !fileComment.isEmpty()) {
                    if (!hasFileComments) {
                        Log.d(TAG, "========== 文件级注释 ==========");
                        hasFileComments = true;
                    }
                    Log.d(TAG, "文件: " + header.getFileName());
                    Log.d(TAG, "注释: " + fileComment);
                    Log.d(TAG, "------------------------");
                }
            }
            if (hasFileComments) {
                Log.d(TAG, "================================");
            }
        } catch (IOException e) {
            Log.e(TAG, "读取ZIP注释失败", e);
        }
    }

    private static String findStripPrefixWithZip4j(File zipFile, String targetDirName, String password) {
        String commonTopFolder = null;
        try (ZipFile zip = new ZipFile(zipFile)) {
            if (password != null && !password.isEmpty()) {
                zip.setPassword(password.toCharArray());
            }
            List<FileHeader> fileHeaders = zip.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                String name = header.getFileName();
                int slashIndex = name.indexOf('/');
                String topFolder = (slashIndex == -1) ? "" : name.substring(0, slashIndex);
                if (commonTopFolder == null) {
                    commonTopFolder = topFolder;
                } else if (!commonTopFolder.equals(topFolder)) {
                    return null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "扫描ZIP条目失败", e);
            return null;
        }
        if (commonTopFolder != null && !commonTopFolder.isEmpty() && commonTopFolder.equals(targetDirName)) {
            return commonTopFolder + "/";
        }
        return null;
    }

    private static boolean isValidTargetFile(File targetFile, File unzipRootDir) {
        try {
            String targetPath = targetFile.getCanonicalPath();
            String rootPath = unzipRootDir.getCanonicalPath();
            return targetPath.startsWith(rootPath + File.separator) || targetPath.equals(rootPath);
        } catch (IOException e) {
            Log.e(TAG, "路径检查异常", e);
            return false;
        }
    }


}