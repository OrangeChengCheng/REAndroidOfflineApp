package com.realengine.offlineuniplugin.retool;

import android.content.Context;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.reclass.FileInfo;

import java.util.List;
import java.util.Map;

public class FileManagerLocal implements FileManagerInterface {

    // ====================== FileUtil 方法 ======================
    @Override
    public FileInfo getAppRootFolder(Context context) {
        return FileUtil.getAppRootFolder(context);
    }

    @Override
    public boolean fileExist(String filePath) {
        return FileUtil.fileExist(filePath);
    }

    @Override
    public boolean fileCreateFolder(String folderPath) {
        return FileUtil.fileCreateFolder(folderPath);
    }

    @Override
    public List<FileInfo> getAllChild(String filePath) {
        return FileUtil.getAllChild(filePath);
    }

    @Override
    public List<FileInfo> getChildFilesBySuffix(String filePath, String suffix) {
        return FileUtil.getChildFilesBySuffix(filePath, suffix);
    }

    @Override
    public boolean deleteAllFiles(String targetPath, boolean keepDir) {
        return FileUtil.deleteAllFiles(targetPath, keepDir);
    }

    @Override
    public boolean copyFile(String sourceFilePath, String targetFilePath) {
        return FileUtil.copyFile(sourceFilePath, targetFilePath);
    }

    @Override
    public void copyFolder(String sourceFolderPath, String targetFolderPath, @Nullable SafUtil.CopyProgressCallback callback) {
    }


    // ====================== DBUtil 方法 ======================
    @Override
    public List<Map<String, Object>> queryToList(String dbPath, String sql, String[] selectionArgs) {
        return DBUtil.queryToList(dbPath, sql, selectionArgs);
    }

    @Override
    public boolean isTableExists(String dbPath, String tableName) {
        return DBUtil.isTableExists(dbPath, tableName);
    }


    // ====================== ZipUtil 方法 ======================
    @Override
    public String unzipFile(String filePath, String password) {
        return ZipUtil.unzipFile(filePath, password);
    }

    @Override
    public void unzipFileAsync(String filePath, String password, @Nullable ZipUtil.UnzipProgressCallback callback) {
        ZipUtil.unzipFileAsync(filePath, password, callback);
    }

    @Override
    public JSONObject readZipCommentsOnly(String filePath) {
        return ZipUtil.readZipCommentsOnly(filePath);
    }
}
