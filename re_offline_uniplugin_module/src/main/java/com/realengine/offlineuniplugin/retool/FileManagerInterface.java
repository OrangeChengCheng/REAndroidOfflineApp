package com.realengine.offlineuniplugin.retool;


import android.content.Context;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.reclass.FileInfo;

import java.util.List;
import java.util.Map;

/**
 * 统一文件操作中间层接口
 * 屏蔽 本地应用存储 / U盘(SAF) 底层差异
 * 对应现有的 FileUtil、DBUtil、ZipUtil
 */
public interface FileManagerInterface {

    // ====================== FileUtil 文件管理 ======================
    FileInfo getAppRootFolder(Context context);
    boolean fileExist(String filePath);
    boolean fileCreateFolder(String folderPath);
    List<FileInfo> getAllChild(String filePath);
    List<FileInfo> getChildFilesBySuffix(String filePath, String suffix);
    boolean deleteAllFiles(String targetPath, boolean keepDir);
    boolean copyFile(String sourceFilePath, String targetFilePath);
    void copyFolder(String sourceFolderPath, String targetFolderPath, @Nullable SafUtil.CopyProgressCallback callback);



    // ====================== DBUtil 数据库操作 ======================
    List<Map<String, Object>> queryToList(String dbPath, String sql, String[] selectionArgs);
    boolean isTableExists(String dbPath, String tableName);




    // ====================== ZipUtil 解压操作 ======================
    String unzipFile(String filePath, String password);
    void unzipFileAsync(String filePath, String password, @Nullable ZipUtil.UnzipProgressCallback callback);
    JSONObject readZipCommentsOnly(String filePath);


}
