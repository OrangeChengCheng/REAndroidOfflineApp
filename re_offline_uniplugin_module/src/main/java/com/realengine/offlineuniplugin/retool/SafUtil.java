package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SafUtil {
    /**
     * 日志TAG
     */
    private static final String TAG = "SafUtil";
    public static final int REQUEST_CODE_PICK_FOLDER = 10002;
    private static final ExecutorService COPY_EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static FolderPickCallback folderPickCallback;

    /**
     * 文件夹选择回调接口
     */
    public interface FolderPickCallback {
        /**
         * 选择授权成功
         * @param rootFolderPath 根目录SAF路径(tree开头)
         */
        void onSuccess(String rootFolderPath);

        /**
         * 选择取消/失败
         */
        void onFailure();
    }

    public static abstract class SimpleFolderPickCallback implements FolderPickCallback {
        @Override public abstract void onSuccess(String rootFolderPath);
        @Override public void onFailure() {}
    }

    /**
     * 复制进度回调接口（所有回调均在主线程执行）
     */
    public interface CopyProgressCallback {
        /** 开始复制，totalFiles 为需要复制的文件总数（不含目录） */
        void onStart(int totalFiles);

        /** 进度更新，processed 当前已复制文件数，currentFileName 当前正在复制的文件名 */
        void onProgress(int processed, int totalFiles, String currentFileName);

        /** 单个文件复制成功 */
        void onFileSuccess(String fileName);

        /** 复制完成，destPath 为本地目标文件夹的绝对路径 */
        void onSuccess(String destPath);

        /** 复制失败 */
        void onFailure(Exception e);
    }

    /**
     * 适配器类，简化回调实现
     */
    public static abstract class SimpleCopyProgressCallback implements CopyProgressCallback {
        @Override public void onStart(int totalFiles) {}
        @Override public void onFileSuccess(String fileName) {}
        @Override public void onFailure(Exception e) {}
        @Override public abstract void onProgress(int processed, int totalFiles, String currentFileName);
        @Override public abstract void onSuccess(String destPath);
    }



    public static void openRootFolderPicker(android.app.Activity activity, FolderPickCallback callback) {
        // 保存回调对象
        folderPickCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        // 授权 读写+持久化权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    // ====================== 2. 【必须调用】处理ActivityResult，触发回调 ======================
    public static void handleActivityResult(int requestCode, int resultCode, Intent data, Context context) {
        // 只处理文件夹选择
        if (requestCode != REQUEST_CODE_PICK_FOLDER || folderPickCallback == null) {
            return;
        }

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            Uri rootUri = data.getData();
            if (rootUri != null) {
                // 持久化权限（使用传入的Context，不再从data里拿）
                saveFolderPermission(context, rootUri);
                // 回调成功，返回根路径
                folderPickCallback.onSuccess(rootUri.toString());
            } else {
                folderPickCallback.onFailure();
            }
        } else {
            // 用户取消或失败
            folderPickCallback.onFailure();
        }
        // 释放回调
        folderPickCallback = null;
    }

    // ====================== 2. 持久化权限（授权后必须调用） ======================
    public static void saveFolderPermission(Context context, Uri treeUri) {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            Log.d(TAG, "权限持久化成功: " + treeUri);
        } catch (Exception e) {
            Log.e(TAG, "权限持久化失败", e);
        }
    }


    /**
     * 根据 SAF 路径字符串获取对应的 DocumentFile 对象（支持混合 URI、树根 URI 和标准文档 URI）。
     * 注意：对于标准文档 URI（不含 /tree/），会降级返回 SingleDocumentFile，可能不支持 listFiles 操作。
     *
     * @param filePath SAF 路径字符串，例如 "content://.../tree/.../document/..." 或纯树根 URI
     * @param context  上下文
     * @return 对应的 DocumentFile 对象，若路径无效或无法解析则返回 null
     */
    public static DocumentFile getDocumentFile(String filePath, Context context) {
        if (filePath == null || filePath.isEmpty()) return null;
        try {
            Uri fullUri = Uri.parse(filePath);
            DocumentFile rootFolder;
            String relativePath = "";

            // 1. 判断类型
            if (filePath.contains("/document/")) {
                // 混合 URI: /tree/.../document/...
                String[] uriParts = filePath.split("/document/");
                String rootTreeUri = uriParts[0];          // 例如 content://.../tree/primary%3AREOutsideSD
                String encodedPath = uriParts[1];          // 例如 primary%3AREOutsideSD%2F...
                rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(rootTreeUri));
                relativePath = encodedPath;
            } else if (filePath.contains("/tree/")) {
                // 纯树根 URI: content://.../tree/...
                rootFolder = DocumentFile.fromTreeUri(context, fullUri);
                relativePath = "";
            } else {
                // 标准文档 URI（没有 /tree/）: 尝试转换为树根+相对路径
                // 此情况在你的场景中可能不会出现，但为了健壮性，尝试通过 DocumentsContract 解析
                // 简单处理：直接返回 fromSingleUri（但这会返回 SingleDocumentFile，不推荐）
                // 更好的做法是：从 URI 中提取文档 ID，再构建混合 URI。因实现复杂且非必须，这里先返回 null 或降级
                return DocumentFile.fromSingleUri(context, fullUri); // 降级，但会丢失 listFiles 能力
            }

            if (rootFolder == null) return null;

            // 2. 逐级查找（跳过根目录自身）
            DocumentFile current = rootFolder;
            if (!relativePath.isEmpty()) {
                String[] segments = relativePath.split("%2F");
                for (int i = 0; i < segments.length; i++) {
                    String seg = segments[i];
                    if (seg.isEmpty()) continue;
                    // 跳过第一段（因为 rootFolder 已经是该目录）
                    if (i == 0) continue;
                    String name = Uri.decode(seg);
                    DocumentFile next = current.findFile(name);
                    if (next == null) return null;
                    current = next;
                }
            }
            return current;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取指定文件所在父目录的 DocumentFile 对象。
     * 优先使用 DocumentFile.getParentFile()，若返回 null 则通过路径解析方式获取。
     *
     * @param filePath 文件的 SAF 路径字符串
     * @param context  上下文
     * @return 父目录的 DocumentFile 对象，若文件无效或父目录不存在则返回 null
     */
    public static DocumentFile getParentFile(String filePath, Context context) {
        DocumentFile file = getDocumentFile(filePath, context);
        if (file == null) return null;

        DocumentFile parentDir = file.getParentFile();
        if (parentDir == null) {
            parentDir = getParentDocumentFile(filePath, context);
        }
        if (parentDir == null || !parentDir.isDirectory()) return null;

        return parentDir;
    }

    /**
     * 通过解析混合 URI 或标准文档 URI 手动获取父目录的 DocumentFile 对象。
     * 当 DocumentFile.getParentFile() 无法正常返回时作为后备方案。
     *
     * @param filePath 文件的 SAF 路径字符串（混合格式或标准格式）
     * @param context  上下文
     * @return 父目录的 DocumentFile 对象，若解析失败则返回 null
     */
    public static DocumentFile getParentDocumentFile(String filePath, Context context) {
        // filePath 是混合格式或标准格式，假设是混合格式
        Uri uri = Uri.parse(filePath);
        List<String> segments = uri.getPathSegments();
        int treeIndex = segments.indexOf("tree");
        int docIndex = segments.indexOf("document");
        if (treeIndex == -1 || docIndex == -1 || docIndex <= treeIndex) {
            // 可能是标准文档 URI，尝试用 DocumentsContract 获取父目录
            return DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(
                    DocumentsContract.buildTreeDocumentUri(uri.getAuthority(),
                            DocumentsContract.getTreeDocumentId(uri)),
                    getParentDocumentId(DocumentsContract.getDocumentId(uri))));
        }
        // 构建树 URI
        Uri.Builder treeBuilder = new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(uri.getAuthority());
        for (int i = 0; i <= docIndex - 1; i++) {
            treeBuilder.appendPath(segments.get(i));
        }
        Uri treeUri = treeBuilder.build();
        // 获取文档 ID（/document/ 之后的所有段）
        StringBuilder docIdBuilder = new StringBuilder();
        for (int i = docIndex + 1; i < segments.size(); i++) {
            if (i > docIndex + 1) docIdBuilder.append('/');
            docIdBuilder.append(segments.get(i));
        }
        String documentId = docIdBuilder.toString(); // 含 %2F
        // 获取父目录 ID：按 %2F 分割，去掉最后一个
        String[] parts = documentId.split("%2F");
        if (parts.length <= 1) {
            // 文件就在根目录下，父目录就是树本身
            return DocumentFile.fromTreeUri(context, treeUri);
        }
        StringBuilder parentIdBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) parentIdBuilder.append("%2F");
            parentIdBuilder.append(parts[i]);
        }
        String parentDocumentId = parentIdBuilder.toString();
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId);
        return DocumentFile.fromSingleUri(context, parentUri);
    }

    /**
     * 从文档 ID 中提取父目录的文档 ID。
     * 例如 "primary:path/to/file" -> "primary:path/to"
     *
     * @param documentId 文档 ID 字符串
     * @return 父目录的文档 ID，若无法提取（无父目录或格式错误）则返回 null
     */
    private static String getParentDocumentId(String documentId) {
        if (documentId == null || documentId.isEmpty()) return null;
        int lastSlash = documentId.lastIndexOf('/');
        if (lastSlash <= 0) return null;
        return documentId.substring(0, lastSlash);
    }

    /**
     * 在指定的目标文件夹下，根据相对路径递归创建子目录。
     * 路径段以正斜杠 '/' 分隔，例如 "a/b/c"。
     *
     * @param destFolder   目标根文件夹（必须为已存在且可写的 DocumentFile 目录）
     * @param relativePath 相对路径字符串，可为空或 null（此时直接返回 destFolder）
     * @return 最终创建或查找到的子目录的 DocumentFile 对象，若创建失败或路径冲突则返回 null
     */
    public static DocumentFile createSafDirectories(DocumentFile destFolder, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return destFolder;
        String[] segments = relativePath.split("/");
        DocumentFile current = destFolder;
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            DocumentFile next = current.findFile(seg);
            if (next == null) {
                next = current.createDirectory(seg);
                if (next == null) {
                    Log.w("Unzip", "Failed to create subdir: " + seg + " under " + current.getUri());
                    return null;
                }
            } else if (!next.isDirectory()) {
                Log.w("Unzip", "Path conflict: " + seg + " is not a directory");
                return null;
            }
            current = next;
        }
        return current;
    }

    /**
     * 根据文件名获取对应的 MIME 类型
     *
     * @param fileName 文件名（包含扩展名，例如 "image.jpg"）
     * @return 对应的 MIME 类型字符串（如 "image/jpeg"），若无法识别则返回 "application/octet-stream"
     */
    public static String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mime != null ? mime : "application/octet-stream";
    }

    /**
     * 将 SAF 授权文件夹及其所有内容递归复制到本地存储指定目录下。
     * 复制后的本地文件夹路径为 localFolderPath + "/" + 原文件夹名称。
     * 支持复制子文件夹和文件，保留原有目录结构。
     *
     * @param safFolderUri    SAF 文件夹的 URI 字符串（例如 content://.../tree/.../document/...）
     * @param localFolderPath 本地目标根目录的绝对路径（例如 /storage/emulated/0/Download）
     * @param context         上下文
     * @return 复制成功复制完成的完整路径，失败返回null
     */
    public static String copyFileSafToLocal(String safFolderUri, String localFolderPath, Context context) {
        return copySafFolderToLocalInternal(safFolderUri, localFolderPath, context, null, -1);
    }

    /**
     * 异步复制 SAF 文件夹到本地存储（支持进度回调，自动在后台线程执行，回调在主线程）
     *
     * @param safFolderUri   SAF 文件夹的 URI 字符串（例如 content://.../tree/.../document/...）
     * @param localFolderPath 本地目标根目录的绝对路径（例如 /storage/emulated/0/Download）
     * @param context        上下文
     * @param callback       进度回调（可为 null）
     */
    public static void copySafFolderToLocalAsync(String safFolderUri, String localFolderPath,
                                                 Context context, @Nullable CopyProgressCallback callback) {
        COPY_EXECUTOR.execute(() -> {
            try {
                // 先统计总文件数（用于进度）
                int totalFiles = -1;
                if (callback != null) {
                    totalFiles = countTotalFilesInSafFolder(safFolderUri, context);
                    final int finalTotal = totalFiles;
                    MAIN_HANDLER.post(() -> callback.onStart(finalTotal));
                }

                String result = copySafFolderToLocalInternal(safFolderUri, localFolderPath, context, callback, totalFiles);
                if (callback != null) {
                    final String finalResult = result;
                    MAIN_HANDLER.post(() -> {
                        if (finalResult != null) {
                            callback.onSuccess(finalResult);
                        } else {
                            callback.onFailure(new Exception("复制失败，返回结果为 null"));
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

    /**
     * 内部同步实现
     */
    @WorkerThread
    private static String copySafFolderToLocalInternal(String safFolderUri, String localFolderPath,
                                                       Context context, @Nullable CopyProgressCallback callback,
                                                       int totalFiles) {
        try {
            // 1. 获取源文件夹 DocumentFile
            DocumentFile sourceDir = getDocumentFile(safFolderUri, context);
            if (sourceDir == null || !sourceDir.isDirectory()) {
                Log.e(TAG, "源文件夹无效或不是目录：" + safFolderUri);
                return null;
            }

            // 2. 获取文件夹名称
            String folderName = sourceDir.getName();
            if (folderName == null || folderName.isEmpty()) {
                Log.e(TAG, "无法获取文件夹名称");
                return null;
            }

            // 3. 构建本地目标目录
            File finalLocalDir = new File(localFolderPath, folderName);
            if (!finalLocalDir.exists()) {
                if (!finalLocalDir.mkdirs()) {
                    Log.e(TAG, "创建本地目录失败：" + finalLocalDir.getAbsolutePath());
                    return null;
                }
            }

            // 4. 如果需要进度但未提供 totalFiles，则先统计
            if (callback != null && totalFiles <= 0) {
                totalFiles = countTotalFilesInSafFolder(sourceDir);
            }

            // 5. 递归复制并统计进度
            AtomicInteger processed = new AtomicInteger(0);
            boolean success = copyRecursive(sourceDir, finalLocalDir, context, callback, totalFiles, processed);

            if (success) {
                Log.d(TAG, "复制完成，目标路径：" + finalLocalDir.getAbsolutePath());
                return finalLocalDir.getAbsolutePath();
            } else {
                Log.e(TAG, "复制过程中发生错误");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "复制异常", e);
            if (callback != null) callback.onFailure(e);
            return null;
        }
    }

    /**
     * 统计 SAF 文件夹下的总文件数（不含目录）
     */
    private static int countTotalFilesInSafFolder(String safFolderUri, Context context) {
        DocumentFile dir = getDocumentFile(safFolderUri, context);
        if (dir == null || !dir.isDirectory()) return 0;
        return countTotalFilesInSafFolder(dir);
    }

    private static int countTotalFilesInSafFolder(DocumentFile dir) {
        int count = 0;
        DocumentFile[] children = dir.listFiles();
        if (children == null) return 0;
        for (DocumentFile child : children) {
            if (child.isDirectory()) {
                count += countTotalFilesInSafFolder(child);
            } else if (child.isFile()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 递归复制文件/文件夹
     *
     * @param source      SAF 源 DocumentFile
     * @param target      本地目标 File
     * @param context     上下文
     * @param callback    进度回调（可为 null）
     * @param totalFiles  总文件数（用于进度分母）
     * @param processed   已处理文件计数器（原子整数）
     * @return 是否全部成功
     */
    private static boolean copyRecursive(DocumentFile source, File target, Context context,
                                         @Nullable CopyProgressCallback callback, int totalFiles,
                                         AtomicInteger processed) {
        if (source.isDirectory()) {
            // 创建目录
            if (!target.exists() && !target.mkdirs()) {
                Log.e(TAG, "创建目录失败：" + target.getAbsolutePath());
                return false;
            }
            DocumentFile[] children = source.listFiles();
            if (children == null) return true;
            for (DocumentFile child : children) {
                File childTarget = new File(target, child.getName());
                if (!copyRecursive(child, childTarget, context, callback, totalFiles, processed)) {
                    return false;
                }
            }
            return true;
        } else if (source.isFile()) {
            // 复制文件
            String fileName = source.getName();
            try (InputStream in = context.getContentResolver().openInputStream(source.getUri());
                 FileOutputStream out = new FileOutputStream(target)) {
                if (in == null) {
                    Log.e(TAG, "无法打开输入流：" + fileName);
                    return false;
                }
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                Log.d(TAG, "复制文件成功：" + fileName);

                // 更新进度
                int current = processed.incrementAndGet();
                if (callback != null && totalFiles > 0) {
                    final int finalCurrent = current;
                    final String finalFileName = fileName;
                    MAIN_HANDLER.post(() -> {
                        callback.onProgress(finalCurrent, totalFiles, finalFileName);
                        callback.onFileSuccess(finalFileName);
                    });
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "复制文件失败：" + fileName, e);
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onFailure(e));
                }
                return false;
            }
        } else {
            Log.w(TAG, "未知类型，跳过：" + source.getUri());
            return true;
        }
    }



}
