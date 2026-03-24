package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ZipOperator {
    private static final String TAG = "ZipOperator";
    private final FileManager fileManager;
    private final Context context;

    public ZipOperator(Context context) {
        this.context = context.getApplicationContext();
        this.fileManager = new FileManager(context);
    }

    /**
     * 解压ZIP文件（可指定密码）
     * @param zipRelativePath ZIP文件的相对路径
     * @param password 解压密码（若ZIP未加密可传null）
     * @return 解压后根目录的绝对路径，失败返回空字符串
     */
    public String unzipFile(String zipRelativePath, String password) {
        Log.d(TAG, "开始解压 → " + zipRelativePath + (password != null ? " (加密)" : ""));

        // 1. 校验ZIP文件是否存在
        if (!fileManager.isFileExist(zipRelativePath)) {
            Log.e(TAG, "ZIP文件不存在：" + zipRelativePath);
            return "";
        }

        // 2. 定位ZIP文件和解压目录
        File zipFile = fileManager.getAppLocFile(zipRelativePath);
        File reOfflineDocDir = zipFile.getParentFile();
        String zipFileName = zipFile.getName();
        String unzipDirName = zipFileName.substring(0, zipFileName.toLowerCase().lastIndexOf(".zip"));
        File unzipRootDir = new File(reOfflineDocDir, unzipDirName);

        // 3. 创建解压根目录
        if (!unzipRootDir.exists() && !unzipRootDir.mkdirs()) {
            Log.e(TAG, "创建解压根目录失败：" + unzipRootDir.getAbsolutePath());
            return "";
        }

        // 4. 获取并打印ZIP注释信息（新增功能）
        readZipComments(zipFile, password);

        // 5. 智能判断是否需要剥离顶层公共文件夹
        String stripPrefix = findStripPrefixWithZip4j(zipFile, unzipDirName, password);
        if (stripPrefix != null) {
            Log.d(TAG, "检测到公共顶层文件夹，将剥离前缀: " + stripPrefix);
        }

        // 6. 执行解压
        return unzipWithZip4j(zipFile, unzipRootDir, stripPrefix, password);
    }


    /**
     * 仅读取ZIP文件的注释信息，不解压
     * @param zipRelativePath ZIP文件相对路径
     * @param password 密码（可为null）
     * @return JSONObject 包含全局注释和文件注释，结构如下：
     *         {
     *             "globalComment": (可能是JSONObject或String，取决于内容是否为有效JSON且非空对象),
     *             "fileComments": [
     *                 {"fileName": "文件1.txt", "comment": "文件1注释"},
     *                 {"fileName": "文件2.jpg", "comment": "文件2注释"}
     *             ]
     *         }
     *         如果发生错误，返回 {"error": "错误信息"}
     */
    public JSONObject readZipCommentsOnly(String zipRelativePath, String password) {
        JSONObject result = new JSONObject();
        if (!fileManager.isFileExist(zipRelativePath)) {
            result.put("error", "ZIP文件不存在");
            return result;
        }

        File zipFile = fileManager.getAppLocFile(zipRelativePath);
        try (ZipFile zip = new ZipFile(zipFile)) {
            if (password != null && !password.isEmpty()) {
                zip.setPassword(password.toCharArray());
            }

            // 处理全局注释
            String globalComment = zip.getComment();
            if (globalComment != null && !globalComment.isEmpty()) {
                try {
                    JSONObject globalObj = JSON.parseObject(globalComment);
                    // 仅当解析后的对象包含至少一个属性时才返回对象，否则返回原始字符串
                    if (globalObj != null && !globalObj.isEmpty()) {
                        result.put("globalComment", globalObj);
                    } else {
                        result.put("globalComment", globalComment);
                    }
                } catch (JSONException e) {
                    // 解析失败，按原始字符串返回
                    result.put("globalComment", globalComment);
                }
            } else {
                result.put("globalComment", "");
            }

            // 文件注释
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
     * 读取并打印ZIP的注释信息（全局注释和文件注释）
     */
    private void readZipComments(File zipFile, String password) {
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
        } catch (ZipException e) {
            Log.e(TAG, "读取ZIP注释失败", e);
        } catch (IOException e) {
            Log.e(TAG, "读取ZIP注释时发生IO异常", e);
        }
    }

    /**
     * 使用 zip4j 扫描ZIP文件，判断是否需要剥离顶层文件夹
     */
    private String findStripPrefixWithZip4j(File zipFile, String targetDirName, String password) {
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
        } catch (ZipException e) {
            Log.e(TAG, "扫描ZIP条目失败", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "扫描ZIP条目时发生IO异常", e);
            return null;
        }

        if (commonTopFolder != null && !commonTopFolder.isEmpty() && commonTopFolder.equals(targetDirName)) {
            return commonTopFolder + "/";
        }
        return null;
    }

    /**
     * 使用 zip4j 执行解压
     */
    private String unzipWithZip4j(File zipFile, File unzipRootDir, String stripPrefix, String password) {
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

                if (header.isDirectory()) {
                    File dir = new File(unzipRootDir, relativePath);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Log.e(TAG, "创建目录失败: " + dir.getAbsolutePath());
                    }
                    continue;
                }

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
                } catch (ZipException e) {
                    Log.e(TAG, "解压文件失败: " + entryName, e);
                }
            }

            Log.d(TAG, "解压完成 | 总条目数：" + entryCount + " | 成功解压文件数：" + successCount);
            Log.d(TAG, "解压目录：" + unzipRootDir.getAbsolutePath());
            return unzipRootDir.getAbsolutePath();

        } catch (ZipException e) {
            Log.e(TAG, "zip4j 解压异常", e);
            return "";
        } catch (IOException e) {
            Log.e(TAG, "解压过程中发生IO异常", e);
            return "";
        }
    }

    /**
     * 检查目标文件是否位于解压根目录之下
     */
    private boolean isValidTargetFile(File targetFile, File unzipRootDir) {
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