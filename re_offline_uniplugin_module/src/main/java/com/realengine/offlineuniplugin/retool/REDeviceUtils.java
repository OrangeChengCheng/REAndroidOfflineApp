package com.realengine.offlineuniplugin.retool;

import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class REDeviceUtils {

    /**
     * 获取底部系统UI高度（包括导航栏/手势区域）
     */
    public static int getBottomSystemUiHeight(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = getWindowInsets(context);
            if (insets != null) {
                return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            }
        }
        return calculateBottomSystemUiBySize(context);
    }

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    /**
     * 通过屏幕尺寸计算底部系统UI高度
     */
    private static int calculateBottomSystemUiBySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        android.graphics.Point realSize = new android.graphics.Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(realSize);
        } else {
            try {
                Method getRealSizeMethod = Display.class.getMethod("getRealSize", android.graphics.Point.class);
                getRealSizeMethod.invoke(display, realSize);
            } catch (Exception e) {
                display.getSize(realSize); // 回退方案
            }
        }

        // 计算可见区域
        View decorView = getDecorView(context);
        if (decorView != null) {
            android.graphics.Rect rect = new android.graphics.Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            return realSize.y - rect.height();
        }

        return 0;
    }

    /**
     * 获取WindowInsets（不依赖Activity）
     */
    private static WindowInsets getWindowInsets(Context context) {
        View decorView = getDecorView(context);
        if (decorView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return decorView.getRootWindowInsets();
            }
        }

        // 备用方案：通过WindowManager获取
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Method getInsetsMethod = Display.class.getMethod("getInsets", int.class);
                return (WindowInsets) getInsetsMethod.invoke(
                        display,
                        WindowInsets.Type.systemBars()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 安全获取DecorView（避免直接强转Activity）
     */
    private static View getDecorView(Context context) {
        try {
            // 尝试通过反射获取Activity的Window
            Method getWindowMethod = context.getClass().getMethod("getWindow");
            Object window = getWindowMethod.invoke(context);

            if (window != null) {
                Method getDecorViewMethod = window.getClass().getMethod("getDecorView");
                return (View) getDecorViewMethod.invoke(window);
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }

        return null;
    }




    /**
     * 获取真实屏幕高度（包含状态栏）
     */
    public static int getRealScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        android.graphics.Point realSize = new android.graphics.Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(realSize);
        } else {
            // 旧版本兼容性处理
            display.getSize(realSize);
        }

        return realSize.y;
    }

    /**
     * dp转换为px
     */
    public static int dpToPx(Context context, float dpValue) {
        final float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }
}
