package com.realengine.offlineuniplugin.retool;

import static io.dcloud.feature.uniapp.common.TypeUniModuleFactory.TAG;

import android.content.Context;
import android.os.Environment;
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
 * 文件管理工具类（修复版：递归获取文件夹所有文件/子文件）
 * 功能：1. 检查文件是否存在 2. 获取文件夹下所有文件（含递归子文件夹）
 */
public class FileManager {
    private static final String TAG = "FileManager";
    // 内部固定AppID
    private static final String FIXED_APP_ID = "__UNI__9A50829";
    private final Context context;
    private static final int BUFFER_SIZE = 1024 * 4; // 4KB缓冲区，提升拷贝效率



    public FileManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<FileInfo> getFileSubList(String filePath) {
        File folder = new File(filePath);
        if (!folder.exists() || !folder.isDirectory()) {
            Log.e(TAG, "文件夹不存在/不是文件夹 → " + folder.getAbsolutePath());
            return null;
        }

        List<FileInfo> allFileList = new ArrayList<>();
        // 递归遍历所有文件
        recursiveScanFolder(folder, allFileList);
        return allFileList;
    }

    public File getAppLocFile(String fileName) {
        File filesDir = context.getApplicationContext().getExternalFilesDir(null);

        if (filesDir == null) {
            Log.e(TAG, "外部存储未挂载，无法获取路径");
            return null;
        }

        String locFilePath_full = filesDir.getAbsolutePath() + "/REOfflineDoc/" + fileName;
        File locFile = new File(locFilePath_full);

        return locFile;
    }


    // 检查文件是否存在（不变）
    public boolean isFileExist(String relativePath) {
        return getAppLocFile(relativePath).exists();
    }



    /**
     * 递归获取UniappSave文件夹下所有文件/子文件夹（含嵌套）
     * @return 所有文件信息列表（空=无文件，null=路径错误）
     */
    public List<FileInfo> getUniappAaveFolderFileList(String fileName) {
        String packageName = context.getPackageName();
        String externalRoot222 = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fullPath222 = externalRoot222 + "/Android/data/" + packageName + "/apps/" + FIXED_APP_ID + "/doc" + fileName;

        File externalFilesDir = context.getExternalFilesDir(null);
        File externalFilesDir_Parent = externalFilesDir.getParentFile();
        if (externalFilesDir_Parent == null) { // 外部存储可能未挂载，需判空避免空指针
            Log.e(TAG, "外部存储未挂载，无法获取路径");
            return new ArrayList<>();
        }
        String externalRoot = externalFilesDir_Parent.getAbsolutePath();
        String fullPath = externalRoot + "/apps/" + FIXED_APP_ID + "/" + "doc" + fileName;
        File folder = new File(fullPath);
        if (!folder.exists() || !folder.isDirectory()) {
            Log.e(TAG, "文件夹不存在/不是文件夹 → " + folder.getAbsolutePath());
            return null;
        }

        List<FileInfo> allFileList = new ArrayList<>();
        // 递归遍历所有文件
        recursiveScanFolder(folder, allFileList);
        Log.d(TAG, "递归获取文件列表成功，总数：" + allFileList.size());
        return allFileList;
    }

    // 递归扫描文件夹（核心：遍历所有子文件/子目录）
    private void recursiveScanFolder(File currentDir, List<FileInfo> fileList) {
        if (currentDir == null || !currentDir.exists() || !currentDir.isDirectory()) return;

        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            fileList.add(new FileInfo(file)); // 封装当前文件/文件夹信息
            // 如果是文件夹，继续递归
//            if (file.isDirectory()) {
//                recursiveScanFolder(file, fileList);
//            }
        }
    }


    public List<FileInfo> getLocFileList(String fileDocName) {
        File filesDir = context.getApplicationContext().getExternalFilesDir(null);

        if (filesDir == null) {
            Log.e(TAG, "外部存储未挂载，无法获取路径");
            return new ArrayList<>();
        }

        String locFilePath_full = filesDir.getAbsolutePath() + "/REOfflineDoc/" + fileDocName;
        File locFile = new File(locFilePath_full);

        List<FileInfo> allFileList = new ArrayList<>();
        // 递归遍历所有文件
        recursiveScanFolder(locFile, allFileList);

        Log.d(TAG, "所有子文件数量：" + allFileList.size());
        for (int i = 0; i < allFileList.size(); i++) {
            FileInfo info = allFileList.get(i);
            Log.d(TAG, "文件[" + i + "]：" + info.fileName);
        }

        Log.d(TAG, "递归获取文件列表成功，总数：" + allFileList.size());
        return allFileList;
    }

    public List<FileInfo> getFileList(String fullPath) {
        File locFile = new File(fullPath);

        List<FileInfo> allFileList = new ArrayList<>();
        // 递归遍历所有文件
        recursiveScanFolder(locFile, allFileList);

        Log.d(TAG, "所有子文件数量：" + allFileList.size());
        for (int i = 0; i < allFileList.size(); i++) {
            FileInfo info = allFileList.get(i);
            Log.d(TAG, "文件[" + i + "]：" + info.fileName);
        }

        Log.d(TAG, "递归获取文件列表成功，总数：" + allFileList.size());
        return allFileList;
    }


    public static boolean copyUniFile(Context context, String uniFileTempPath) {
        File filesDir = context.getApplicationContext().getExternalFilesDir(null);

        if (filesDir == null) {
            Log.e(TAG, "外部存储未挂载，无法获取路径");
            return false;
        }

        File uniFile_root = filesDir.getParentFile();

        String uniFileTempPath_handle = uniFileTempPath.startsWith("_") ? uniFileTempPath.substring(1) : uniFileTempPath;
        String uniFileTempPath_full = uniFile_root.getAbsolutePath() + "/apps/" + FIXED_APP_ID + "/" + uniFileTempPath_handle;
        File uniFileTemp = new File(uniFileTempPath_full);


        String targetFilePath_full = filesDir.getAbsolutePath() + "/REOfflineDoc/" + uniFileTemp.getName();

//        File targetFile = new File(filesDir, uniFileTemp.getName());
        File targetFile = new File(targetFilePath_full);


        return copyFile(uniFileTemp, targetFile);
    }



    /**
     * 拷贝文件夹（递归）
     * @param sourceDir 源文件夹路径（A路径）
     * @param targetDir 目标文件夹路径（B路径）
     * @return true=拷贝成功，false=拷贝失败
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
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return true=成功，false=失败
     */
    private static boolean copyFile(File sourceFile, File targetFile) {
        InputStream is = null;
        OutputStream os = null;
        try {
            // ========== 新增：自动创建目标文件的父目录（关键修复） ==========
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                // mkdirs()：创建多级目录（比如a/b/c，即使a、b都不存在也能一次性创建）
                // 区别于mkdir()：只能创建单级目录，父目录不存在则失败
                boolean isDirCreated = parentDir.mkdirs();
                if (!isDirCreated) {
                    Log.e(TAG, "目标文件父目录创建失败：" + parentDir.getAbsolutePath());
                    return false;
                }
            }
            // ==============================================================

            // 此时父目录已存在，可安全创建文件流
            is = new FileInputStream(sourceFile);
            os = new FileOutputStream(targetFile);

            // 缓冲区拷贝文件内容
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
            // 关闭流（必须释放资源）
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }


    /**
     * 删除指定路径下的所有内容（递归）
     * @param targetPath 目标文件/文件夹路径
     * @param keepDir 是否保留原文件夹（true=只删内容，false=删除文件夹+内容）
     * @return true=删除成功/无文件可删，false=删除失败
     */
    public static boolean deleteAllFiles(String targetPath, boolean keepDir) {
        File targetFile = new File(targetPath);
        return deleteAllFiles(targetFile, keepDir);
    }

    /**
     * 重载方法（接收File对象，核心实现）
     */
    public static boolean deleteAllFiles(File targetFile, boolean keepDir) {
        // 1. 目标不存在，直接返回成功
        if (!targetFile.exists()) {
            Log.d(TAG, "目标路径不存在，无需删除：" + targetFile.getAbsolutePath());
            return true;
        }

        // 2. 是文件：直接删除
        if (targetFile.isFile()) {
            boolean isDeleted = targetFile.delete();
            if (isDeleted) {
                Log.d(TAG, "删除文件成功：" + targetFile.getAbsolutePath());
            } else {
                Log.e(TAG, "删除文件失败：" + targetFile.getAbsolutePath());
            }
            return isDeleted;
        }

        // 3. 是文件夹：递归删除所有子文件/子文件夹
        File[] files = targetFile.listFiles();
        if (files == null) { // 无权限访问或文件夹为空
            Log.w(TAG, "无法遍历文件夹：" + targetFile.getAbsolutePath());
            // 若要保留文件夹，返回true；否则删除空文件夹
            return keepDir || targetFile.delete();
        }

        boolean allSuccess = true;
        for (File file : files) {
            // 递归删除子项（子项无论文件/文件夹，都删除内容+本身）
            boolean subSuccess = deleteAllFiles(file, false);
            if (!subSuccess) {
                allSuccess = false;
            }
        }

        // 4. 是否保留原文件夹：不保留则删除空文件夹
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


    public static List<String> getSubFolderNames(String folderPath) {
        List<String> folderNames = new ArrayList<>();
        File folder = new File(folderPath);

        // 检查路径是否存在且是一个目录
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles(); // 获取所有子项
            if (files != null) { // 防止空文件夹或权限问题导致返回 null
                for (File file : files) {
                    if (file.isDirectory()) {
                        folderNames.add(file.getName());
                    }
                }
            }
        } else {
            // 处理路径无效的情况（可选）
            System.err.println("指定的路径不是有效目录: " + folderPath);
        }
        return folderNames;
    }

}