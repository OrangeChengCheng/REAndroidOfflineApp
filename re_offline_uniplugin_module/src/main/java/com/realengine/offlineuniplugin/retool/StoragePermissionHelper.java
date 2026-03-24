package com.realengine.offlineuniplugin.retool;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 存储权限辅助类（静态方法版）
 * 提供同步检查权限和发起授权请求的方法
 */
public class StoragePermissionHelper {
    private static final String TAG = "StoragePermissionHelper";

    /**
     * 检查当前是否已有存储权限
     * @param context 上下文
     * @return true 有权限，false 无权限
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= 23) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // 低版本默认有权限
        }
    }

    /**
     * 请求存储权限
     * 如果已有权限，立即返回 true；否则自动跳转授权页面（或请求运行时权限）
     * 注意：此方法不提供回调，调用方需在 onResume 中再次检查权限状态
     * @param activity 当前 Activity
     * @return true 表示已有权限（无需跳转）；false 表示已发起授权请求（需等待用户操作）
     */
    public static boolean requestStoragePermission(Activity activity) {
        if (activity == null) {
            Log.e(TAG, "requestStoragePermission: activity is null");
            return false;
        }

        if (hasStoragePermission(activity)) {
            Log.d(TAG, "已有存储权限");
            return true;
        }

        // 无权限，发起请求
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11+：跳转 MANAGE_EXTERNAL_STORAGE 设置页
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
            Log.d(TAG, "已跳转所有文件访问权限设置页");
        } else if (Build.VERSION.SDK_INT >= 23) {
            // Android 6.0-10：请求运行时权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    1001); // requestCode 可自定义，但需在 Activity 的 onRequestPermissionsResult 中处理
            Log.d(TAG, "已请求运行时权限");
        } else {
            // 低版本不应走到这里，因为 hasStoragePermission 已返回 true
        }
        return false;
    }
}