package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.text.TextUtils;

public class FileManager {
    public static final int TYPE_LOCAL = 0;
    public static final int TYPE_SAF = 1;

    // 统一获取文件操作器
    public static FileManagerInterface getOperator(Context context, String... filePath) {
        // 情况1：无路径（仅 getAppRootFolder）→ 固定返回本地操作器
        if (filePath == null || filePath.length == 0 || TextUtils.isEmpty(filePath[0])) {
            return new FileManagerLocal();
        }

        // 情况2：有路径 → 内部自动判断 本地 / U盘
        String path = filePath[0];
        if (path.startsWith("content://")) {
            // U盘/外置存储：内部创建SAF操作器
            return new FileManagerSaf(context);
        } else {
            // 本地存储：内部创建本地操作器
            return new FileManagerLocal();
        }
    }
}
