package com.realengine.offlineuniplugin.reui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.realengine.offlineuniplugin.R;


public class RETip {
    public enum RETip_Level {
        RETip_L0,//父组件中心
        RETip_L1,//一级面板上方
        RETip_L2,//二级面板上方
    }
    private static final float RETipSpaceY_L1 = 150;
    private static final float RETipSpaceY_L2 = 215;
    private static final float RETipHeight =50F;
    private static final int RETipStaticDelay = 800;
    private static final int RETipAnimateTime =500;
    private static RETip curr_tip = null;
    private Context context;
    private ViewGroup main_layout;
    private View re_tip;
    private TextView re_tip_text;

    public RETip(Context context, ViewGroup main_layout, String message, RETip_Level level) {
        this.context = context;
        this.main_layout = main_layout;
        re_tip = LayoutInflater.from(context).inflate(R.layout.re_tip_layout, main_layout, false);
        re_tip_text = re_tip.findViewById(R.id.re_tip_text);
        re_tip_text.setText(message);
        re_tip_text.setAlpha(0);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, // 宽度匹配父布局
                ConstraintLayout.LayoutParams.WRAP_CONTENT  // 高度包裹内容
        );
        re_tip.setLayoutParams(params);
        setTipPosition(level);
        this.main_layout.addView(re_tip);
        re_tip.setVisibility(View.GONE);
    }

    public static void showRETip(Context context, ViewGroup main_layout, String message, RETip_Level level) {
        if (curr_tip != null) {
            curr_tip.clear();
        }
        RETip popupView = new RETip(context, main_layout, message, level);
        popupView.showStatic();
        curr_tip = popupView;
    }

    private void showStatic() {
        if (re_tip.getVisibility() != View.VISIBLE) {
            re_tip.setVisibility(View.VISIBLE);
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(re_tip_text, "alpha", 0f, 1f);
        animator.setDuration(RETipAnimateTime);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, RETipStaticDelay);
    }

    private void setTipPosition(RETip_Level level) {
        int height = main_layout.getHeight();
        // 获取当前屏幕的DisplayMetrics对象
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int heightInPixels = height;
        // 将像素转换为dp
        float density = displayMetrics.density;
        int heightInDp = (int) (heightInPixels / density);

        switch (level) {
            // 中间位置（垂直居中）
            case RETip_L1: // 距离底部100dp
                float re_tip_l1 = heightInDp - RETipSpaceY_L1;
                re_tip.setTranslationY((int) (re_tip_l1 * density));
                break;
            case RETip_L2: // 距离底部300dp
                float re_tip_l2 = heightInDp - RETipSpaceY_L2;
                re_tip.setTranslationY((int) (re_tip_l2 * density));
                break;
            default:
                float re_tip_l0 = heightInDp / 2 - (RETipHeight / 2);
                re_tip.setTranslationY((int) (re_tip_l0 * density));
                break;
        }
    }

    public void showHeightAnimate() {
        if (re_tip.getVisibility() != View.VISIBLE) {
            re_tip.setVisibility(View.VISIBLE);
        }

        int height = main_layout.getHeight();
        // 获取当前屏幕的DisplayMetrics对象
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int heightInPixels = height;
        // 将像素转换为dp
        float density = displayMetrics.density;
        int heightInDp = (int) (heightInPixels / density);

        float tran_startY = (int) ((heightInDp - RETipSpaceY_L1) * density);
        float tran_endY = (int) ((heightInDp - RETipSpaceY_L2) * density);;
        re_tip.setTranslationY(tran_startY);
        ObjectAnimator animator = ObjectAnimator.ofFloat(re_tip, "translationY", tran_startY, tran_endY);
        animator.setDuration(RETipAnimateTime);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, RETipStaticDelay);
    }

    public void hide() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(re_tip_text, "alpha", 1f, 0f);
        animator.setDuration(RETipAnimateTime);
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                clear();
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    private void clear() {
        re_tip.setVisibility(View.GONE);
        main_layout.removeView(re_tip);
        curr_tip = null;
    }
}
