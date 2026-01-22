package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Android ZIP解压工具类
 * 功能：将ZIP文件解压到APP沙盒指定目录，支持进度回调、异常处理
 */
public class ZipUnzipUtils {
    private static final String TAG = "ZipUnzipUtils";
    private static final int BUFFER_SIZE = 8192; // 缓冲区大小，提升解压效率

    // 解压进度回调接口
    public interface UnzipListener {
        void onProgress(int progress); // 进度0-100
        void onSuccess(); // 解压成功
        void onFailure(String errorMsg); // 解压失败
    }

    /**
     * 异步解压ZIP文件到指定目录（避免主线程阻塞）
     * @param context       上下文，用于获取沙盒路径
     * @param zipFilePath   ZIP文件的完整路径（如：/data/data/包名/files/resource.zip）
     * @param targetDirName 沙盒内的目标目录名（如："unzip_res"，最终路径为context.filesDir/unzip_res）
     * @param listener      解压回调
     */
    public static void unzipAsync(Context context, String zipFilePath, String targetDirName, UnzipListener listener) {
        new Thread(() -> {
            try {
                // 获取沙盒内的目标目录完整路径
                File targetDir = new File(context.getFilesDir(), targetDirName);
                unzip(zipFilePath, targetDir.getAbsolutePath(), listener);

                // 主线程回调成功
                new Handler(Looper.getMainLooper()).post(listener::onSuccess);
            } catch (Exception e) {
                String errorMsg = "解压失败：" + e.getMessage();
                Log.e(TAG, errorMsg, e);
                // 主线程回调失败
                new Handler(Looper.getMainLooper()).post(() -> listener.onFailure(errorMsg));
            }
        }).start();
    }

    /**
     * 同步解压核心方法
     * @param zipFilePath ZIP文件路径
     * @param targetPath  目标解压路径
     * @param listener    进度回调
     * @throws IOException 解压异常
     */
    private static void unzip(String zipFilePath, String targetPath, UnzipListener listener) throws IOException {
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            throw new IOException("ZIP文件不存在：" + zipFilePath);
        }

        // 创建目标目录（不存在则创建，包括多级目录）
        File targetDir = new File(targetPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("无法创建目标解压目录：" + targetPath);
        }

        // 打开ZIP文件，计算总文件数（用于进度计算）
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        int total = 0;
        while (entries.hasMoreElements()) {
            entries.nextElement();
            total++;
        }
        entries = zip.entries(); // 重置枚举器
        int current = 0;

        // 逐行解压每个文件/目录
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String entryPath = targetPath + File.separator + entryName;

            // 如果是目录，直接创建
            if (entry.isDirectory()) {
                File dir = new File(entryPath);
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.w(TAG, "创建目录失败：" + entryPath);
                }
                current++;
                updateProgress(listener, current, total);
                continue;
            }

            // 如果是文件，读取并写入目标路径
            try (InputStream in = new BufferedInputStream(zip.getInputStream(entry));
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(entryPath))) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            } catch (Exception e) {
                Log.e(TAG, "解压文件失败：" + entryName, e);
                throw new IOException("解压文件失败：" + entryName, e);
            }

            // 更新进度
            current++;
            updateProgress(listener, current, total);
        }

        zip.close(); // 关闭ZIP文件流
    }

    /**
     * 更新解压进度（主线程回调）
     */
    private static void updateProgress(UnzipListener listener, int current, int total) {
        if (listener == null || total == 0) return;
        int progress = (int) (((float) current / total) * 100);
        new Handler(Looper.getMainLooper()).post(() -> listener.onProgress(progress));
    }

    /**
     * 辅助方法：获取APP沙盒内的ZIP解压目录（简化调用）
     * @param context 上下文
     * @param dirName 自定义目录名
     * @return 完整路径
     */
    public static String getUnzipDirInSandbox(Context context, String dirName) {
        return new File(context.getFilesDir(), dirName).getAbsolutePath();
    }
}
