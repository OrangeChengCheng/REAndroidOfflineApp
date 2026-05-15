package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.reclass.FileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileManagerSaf implements FileManagerInterface {
    private final Context mContext;

    // 构造方法：必须传入上下文
    public FileManagerSaf(Context context) {
        this.mContext = context.getApplicationContext();
    }


    // ====================== 工具方法：String Path → Uri → DocumentFile ======================
    private DocumentFile getDocumentFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;

        try {
            Uri fullUri = Uri.parse(filePath);
            DocumentFile rootFolder;
            String relativePath = "";

            // 1. 自动判断：根目录 / 子文件夹
            if (filePath.contains("/document/")) {
                // ========== 子文件夹路径（带 /document/）：自动拆分根目录 + 相对路径 ==========
                String[] uriParts = filePath.split("/document/");
                String rootTreeUri = uriParts[0];          // 自动提取根目录
                String encodedPath = uriParts[1];          // 自动提取相对路径

                // 解析根目录
                rootFolder = DocumentFile.fromTreeUri(mContext, Uri.parse(rootTreeUri));
                // 根目录下的相对路径
                relativePath = encodedPath;
            } else {
                // ========== 根目录路径（不带 /document/）：直接使用 ==========
                rootFolder = DocumentFile.fromTreeUri(mContext, fullUri);
                relativePath = "";
            }

            if (rootFolder == null) return null;

            // 2. 自动逐级查找目标文件夹（核心：得到支持遍历的文件夹）
            DocumentFile currentFolder = rootFolder;
            if (!relativePath.isEmpty()) {
                // 切割编码后的路径
                String[] pathSegments = relativePath.split("%2F");
                for (int i = 0; i < pathSegments.length; i++) {
                    String seg = pathSegments[i];
                    if (seg.isEmpty()) continue;

                    // 🔥🔥🔥 终极修复：跳过第一段（根目录自身，不需要查找）
                    if (i == 0) {
                        continue;
                    }

                    // 解码名称，查找真实子文件夹
                    String name = Uri.decode(seg);
                    currentFolder = currentFolder.findFile(name);
                    if (currentFolder == null) {
                        return null;
                    }
                }
            }

            return currentFolder;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // ====================== FileUtil 方法实现（SAF 版）======================
    @Override
    public FileInfo getAppRootFolder(Context context) {
        // U盘模式不使用应用根目录，返回 null 或按业务处理
        return null;
    }

    @Override
    public boolean fileExist(String filePath) {
        DocumentFile file = SafUtil.getDocumentFile(filePath, mContext);
        return file != null && file.exists();
    }

    @Override
    public boolean fileCreateFolder(String folderPath) {
        DocumentFile parent = getDocumentFile(folderPath);
        return parent != null && parent.exists() && parent.isDirectory();
    }

    @Override
    public List<FileInfo> getAllChild(String filePath) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        // 使用新方法（注意传入 context，可以是成员变量或参数）
        DocumentFile folder = SafUtil.getDocumentFile(filePath, mContext);
        if (folder == null || !folder.isDirectory()) {
            return fileInfoList;
        }
        for (DocumentFile child : folder.listFiles()) {
            FileInfo info = new FileInfo(child);
            if (info != null) {
                fileInfoList.add(info);
            }
        }
        return fileInfoList;
    }

    @Override
    public List<FileInfo> getChildFilesBySuffix(String filePath, String suffix) {
        List<FileInfo> allChild = getAllChild(filePath);
        List<FileInfo> result = new java.util.ArrayList<>();

        for (FileInfo info : allChild) {
            if (!info.directory && info.fileName.endsWith(suffix)) {
                result.add(info);
            }
        }
        return result;
    }

    @Override
    public boolean deleteAllFiles(String targetPath, boolean keepDir) {
        DocumentFile file = getDocumentFile(targetPath);
        if (file == null) return false;

        if (keepDir) {
            // 只删除内容，保留文件夹
            DocumentFile[] children = file.listFiles();
            if (children == null) return true;

            boolean success = true;
            for (DocumentFile child : children) {
                if (!child.delete()) {
                    success = false;
                }
            }
            return success;
        } else {
            // 删除整个文件夹
            return file.delete();
        }
    }

    @Override
    public boolean copyFile(String sourceFilePath, String targetFilePath) {
        return false;
    }

    @Override
    public void copyFolder(String sourceFolderPath, String targetFolderPath, @Nullable SafUtil.CopyProgressCallback callback) {
        SafUtil.copySafFolderToLocalAsync(sourceFolderPath, targetFolderPath, mContext, callback);
    }


    // ====================== DBUtil （流实现，后续完善）======================
    @Override
    public List<Map<String, Object>> queryToList(String dbPath, String sql, String[] selectionArgs) {
        // 预留：SAF 读取 DB 流实现（后续步骤实现）
        return new java.util.ArrayList<>();
    }

    @Override
    public boolean isTableExists(String dbPath, String tableName) {
        // 预留：后续实现
        return false;
    }


    // ====================== ZipUtil ======================
    @Override
    public String unzipFile(String filePath, String password) {
        return ZipUtil.unzipFileFromStream(filePath, password, mContext);
    }

    @Override
    public void unzipFileAsync(String filePath, String password, @Nullable ZipUtil.UnzipProgressCallback callback) {
        ZipUtil.unzipFileFromStreamAsync(filePath, password, mContext, callback);
    }

    @Override
    public JSONObject readZipCommentsOnly(String filePath) {
        return ZipUtil.readZipCommentsOnlyFromStream(filePath, mContext);
    }




}
