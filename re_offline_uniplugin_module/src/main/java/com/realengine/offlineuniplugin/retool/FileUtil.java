package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.util.Log;

import com.realengine.offlineuniplugin.reclass.FileInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件管理工具类
 * <p>
 * 核心功能：
 * 1. 文件/文件夹基础判断（存在、父目录、子文件）
 * 2. 按后缀筛选文件
 * 3. 单个文件/递归文件夹拷贝
 * 4. 递归删除文件/文件夹（支持保留目录）
 * 5. 获取子文件夹名称列表
 * 6. 适配UniApp离线插件的文件拷贝专用方法
 * <p>
 * 所有方法均为静态/工具方法，无需实例化（除构造方法外），线程安全。
 *
 * @author 自动生成
 * @date 2025
 */
public class FileUtil {
    /**
     * 日志TAG
     */
    private static final String TAG = "FileUtil";

    /**
     * 内部固定UniApp应用ID（插件专属配置）
     */
    private static final String FIXED_APP_ID = "__UNI__9A50829";

    /**
     * 文件拷贝缓冲区大小：4KB，平衡内存占用与拷贝效率
     */
    private static final int BUFFER_SIZE = 1024 * 4;

    /**
     * 构造方法
     *
     * @param context 上下文，内部自动转为ApplicationContext
     */
    public FileUtil() {

    }

    /**
     * 获取沙盒默认存储路径（外部存储）
     *
     * @param context         上下文
     * @return 路径地址
     */
    public static FileInfo getAppRootFolder(Context context) {
        File externalFilesDir = context.getApplicationContext().getExternalFilesDir(null);
        if (externalFilesDir == null) {
            Log.e(TAG, "外部存储未挂载，无法获取路径");
            return null;
        }

        File rootFolder = externalFilesDir.getParentFile();
        if (rootFolder == null) {
            Log.e(TAG, "应用根目录获取失败");
            return null;
        }
        return new FileInfo(rootFolder);
    }

    /**
     * 判断文件/文件夹是否存在
     *
     * @param filePath 文件/文件夹绝对路径
     * @return 存在返回true，不存在返回false
     */
    public static boolean fileExist(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * 获取指定路径的父文件夹信息
     *
     * @param filePath 文件/文件夹路径
     * @return 父文件夹FileInfo对象；若为根目录/路径不存在则返回null
     */
    public static FileInfo getParentFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                return new FileInfo(file);
            } else {
                Log.e(TAG, "该路径是根目录，没有父文件夹 → " + filePath);
            }
        } else {
            Log.e(TAG, "路径不存在 → " + filePath);
        }
        return null;
    }

    /**
     * 获取文件夹下所有直接子项（文件+文件夹），不递归子文件夹
     *
     * @param filePath 文件夹路径
     * @return 子项FileInfo列表；路径无效/无权限时返回null
     */
    public static List<FileInfo> getAllChild(String filePath) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        File folder = new File(filePath);

        if (!folder.exists()) {
            Log.e(TAG, "路径不存在 → " + filePath);
            return null;
        }
        if (!folder.isDirectory()) {
            Log.e(TAG, "路径不是文件夹 → " + filePath);
            return null;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return fileInfoList;
        }
        for (File file : files) {
            fileInfoList.add(new FileInfo(file));
        }
        return fileInfoList;
    }

    /**
     * 获取文件夹内指定后缀的文件列表（仅直接子文件，不递归）
     *
     * @param filePath 文件夹路径
     * @param suffix   文件后缀名（例：.db、.jpg、.txt）
     * @return 符合后缀的文件信息集合，路径无效时返回空列表
     */
    public static List<FileInfo> getChildFilesBySuffix(String filePath, String suffix) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        File folder = new File(filePath);

        // 判断路径是否存在
        if (!folder.exists()) {
            Log.e(TAG, "路径不存在 → " + filePath);
            return fileInfoList;
        }
        // 判断是否是文件夹
        if (!folder.isDirectory()) {
            Log.e(TAG, "路径不是文件夹 → " + filePath);
            return fileInfoList;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return fileInfoList;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(suffix)) {
                fileInfoList.add(new FileInfo(file));
            }
        }

        return fileInfoList;
    }

    /**
     * 递归拷贝整个文件夹（包含所有子文件、子文件夹）
     * 自动创建目标目录，支持多级目录拷贝
     *
     * @param sourceDir 源文件夹
     * @param targetDir 目标文件夹
     * @return 全部拷贝成功返回true，任意一步失败返回false
     */
    public static boolean copyDirectory(File sourceDir, File targetDir) {
        // 1. 校验源文件夹是否存在且是文件夹
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            Log.e(TAG, "源文件夹不存在或不是文件夹：" + sourceDir.getAbsolutePath());
            return false;
        }

        // 2. 创建目标文件夹（如果不存在）
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs(); // mkdirs()创建多级目录，mkdir()仅创建单级
            if (!isCreated) {
                Log.e(TAG, "目标文件夹创建失败：" + targetDir.getAbsolutePath());
                return false;
            }
        }

        // 3. 遍历源文件夹下的所有文件/子文件夹
        File[] files = sourceDir.listFiles();
        if (files == null) { // 源文件夹为空或无权限访问
            Log.e(TAG, "无法遍历源文件夹：" + sourceDir.getAbsolutePath());
            return false;
        }

        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                // 4. 递归拷贝子文件夹
                if (!copyDirectory(file, targetFile)) {
                    return false;
                }
            } else {
                // 5. 拷贝单个文件
                if (!copyFile(file, targetFile)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 拷贝单个文件
     * 自动创建目标文件父目录，使用缓冲区提升效率
     *
     * @param sourceFilePath 源文件路径
     * @param targetFilePath 目标文件路径
     * @return 拷贝成功返回true，失败返回false
     */
    public static boolean copyFile(String sourceFilePath, String targetFilePath) {
        File sourceFile = new File(sourceFilePath);
        File targetFile = new File(targetFilePath);
        return copyFile(sourceFile, targetFile);
    }

    /**
     * 拷贝单个文件（内部核心方法）
     * 自动创建目标文件父目录，使用缓冲区提升效率
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return 拷贝成功返回true，失败返回false
     */
    private static boolean copyFile(File sourceFile, File targetFile) {
        InputStream is = null;
        OutputStream os = null;
        try {
            // 自动创建目标文件的父目录（关键修复）
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                // mkdirs()：创建多级目录；mkdir()：仅创建单级目录
                boolean isDirCreated = parentDir.mkdirs();
                if (!isDirCreated) {
                    Log.e(TAG, "目标文件父目录创建失败：" + parentDir.getAbsolutePath());
                    return false;
                }
            }

            is = new FileInputStream(sourceFile);
            os = new FileOutputStream(targetFile);

            // 缓冲区拷贝
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "拷贝文件失败：" + sourceFile.getAbsolutePath() + " → " + targetFile.getAbsolutePath(), e);
            return false;
        } finally {
            // 确保流关闭，释放资源
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }

    /**
     * 递归删除指定路径下的所有内容
     *
     * @param targetPath 目标文件/文件夹绝对路径
     * @param keepDir    是否保留顶层文件夹：true=只删内容保留目录；false=删除目录+内容
     * @return 删除成功/无文件可删返回true；删除失败返回false
     */
    public static boolean deleteAllFiles(String targetPath, boolean keepDir) {
        File targetFile = new File(targetPath);
        return deleteAllFiles(targetFile, keepDir);
    }

    /**
     * 递归删除文件/文件夹（核心实现，重载方法）
     *
     * @param targetFile 目标File对象
     * @param keepDir    是否保留顶层目录
     * @return 删除结果
     */
    public static boolean deleteAllFiles(File targetFile, boolean keepDir) {
        // 目标不存在，无需删除
        if (!targetFile.exists()) {
            Log.d(TAG, "目标路径不存在，无需删除：" + targetFile.getAbsolutePath());
            return true;
        }

        // 是文件：直接删除
        if (targetFile.isFile()) {
            boolean isDeleted = targetFile.delete();
            if (isDeleted) {
                Log.d(TAG, "删除文件成功：" + targetFile.getAbsolutePath());
            } else {
                Log.e(TAG, "删除文件失败：" + targetFile.getAbsolutePath());
            }
            return isDeleted;
        }

        // 是文件夹：递归删除子项
        File[] files = targetFile.listFiles();
        if (files == null) {
            Log.w(TAG, "无法遍历文件夹：" + targetFile.getAbsolutePath());
            return keepDir || targetFile.delete();
        }

        boolean allSuccess = true;
        for (File file : files) {
            boolean subSuccess = deleteAllFiles(file, false);
            if (!subSuccess) {
                allSuccess = false;
            }
        }

        // 是否保留原文件夹
        if (!keepDir) {
            boolean dirDeleted = targetFile.delete();
            if (dirDeleted) {
                Log.d(TAG, "删除文件夹成功：" + targetFile.getAbsolutePath());
            } else {
                Log.e(TAG, "删除文件夹失败：" + targetFile.getAbsolutePath());
                allSuccess = false;
            }
        } else {
            Log.d(TAG, "保留原文件夹，仅删除内容：" + targetFile.getAbsolutePath());
        }

        return allSuccess;
    }

    /**
     * 获取指定目录下所有直接子文件夹的名称列表
     * 不递归、不包含文件，仅返回一级子文件夹名
     *
     * @param folderPath 目标文件夹路径
     * @return 子文件夹名称列表，路径无效时返回空列表
     */
    public static List<String> getSubFolderNames(String folderPath) {
        List<String> folderNames = new ArrayList<>();
        File folder = new File(folderPath);

        // 检查路径是否存在且是一个目录
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderNames.add(file.getName());
                    }
                }
            }
        } else {
            System.err.println("指定的路径不是有效目录: " + folderPath);
        }
        return folderNames;
    }
}