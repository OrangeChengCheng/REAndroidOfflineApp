package com.realengine.offlineuniplugin.reclass;

import java.io.File;

/**
 * 文件信息实体类
 * 封装文件/文件夹的核心信息
 */
public class FileInfo {
    public String fileName;       // 文件名（含后缀）
    public String filePath;       // 文件完整路径
    public long fileSize;         // 文件大小（字节）
    public String fileSizeDesc;   // 文件大小描述（如：42.7MB）
    public String fileType;       // 文件类型（后缀，文件夹为"directory"）
    public boolean isDirectory;   // 是否是文件夹
    public long lastModifyTime;   // 最后修改时间（时间戳）

    // 构造方法
    public FileInfo(File file) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.fileSizeDesc = formatFileSize(fileSize);
        this.isDirectory = file.isDirectory();
        this.lastModifyTime = file.lastModified();
        this.fileType = isDirectory ? "directory" : getFileType(fileName);
    }

    // 格式化文件大小（字节 → 易读格式）
    private String formatFileSize(long size) {
        if (size <= 0) return "0B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s",
                size / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    // 获取文件类型（后缀）
    private String getFileType(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "unknown" : fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    // Getter方法（按需生成）
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
    public String getFileSizeDesc() { return fileSizeDesc; }
    public String getFileType() { return fileType; }
    public boolean isDirectory() { return isDirectory; }
    public long getLastModifyTime() { return lastModifyTime; }

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSizeDesc='" + fileSizeDesc + '\'' +
                ", fileType='" + fileType + '\'' +
                ", isDirectory=" + isDirectory +
                '}';
    }
}
