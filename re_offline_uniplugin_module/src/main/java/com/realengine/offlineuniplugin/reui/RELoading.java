package com.realengine.offlineuniplugin.reui;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.realengine.offlineuniplugin.R;

import java.util.Random;

public class RELoading {
    private Context context;
    private ViewGroup main_layout;
    private View loadingView;
    private ProgressBar progressBar;
    private TextView tipTextView;
    private ImageView loadingImageView;
    private Handler handler;
    private Runnable runnable;
    private String[] tips = {
            "推荐使用谷歌、Edge、火狐浏览器登录黑洞引擎",
            "你知道吗，模型的加载速度与它的大小没有直接关系，而与它的三角面片数相关。三角面片数越少，加载速度越快",
            "小tips:按住ctrl键的同时：鼠标点击，代表多选；鼠标拖动，代表框选",
            "小tips:想要批量隐藏构件，可以选中目录树某一层级再点击【隐藏构件】",
            "小tips:引擎工具栏的【地形透明度】仅对GIS数据生效",
            "小tips:WASD可以进行漫游操作，Q和E控制上升和下降",
            "小tips:场景范围过大怎么办，点击目录树可以快速定位",
            "小tips:鼠标左键代表平移，右键代表旋转，按住中键可绕指定点旋转"
    };

    public RELoading(Context context, ViewGroup main_layout) {
        this.context = context;
        this.main_layout = main_layout;
        // 初始化 loadingView，但暂时不添加到任何布局中
        loadingView = LayoutInflater.from(context).inflate(R.layout.re_loading_layout, main_layout, false);
        // 将loadingView添加到mainLayout中，并设置约束以覆盖全部
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        loadingView.setLayoutParams(params);
        // 将 loadingView 添加到父布局中
        this.main_layout.addView(loadingView);

        loadingImageView = loadingView.findViewById(R.id.re_IV_loading);
        Glide.with(this.context).asGif().load(R.drawable.loading).into(loadingImageView);
        progressBar = loadingView.findViewById(R.id.re_progress_loading);
        tipTextView = loadingView.findViewById(R.id.re_TV_tip);
        handler = new Handler();
        show();
    }

    public void show() {
        loadingView.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        startUpdatingTip();
    }

    public void hide() {
        loadingView.setVisibility(View.GONE);
        stopUpdatingTip();
    }

    public void updateProgress(int progress) {
        progressBar.setProgress(progress);
    }

    public void startUpdatingTip() {
        runnable = new Runnable() {
            @Override
            public void run() {
                // 更新TextView的内容
                int randomIndex = new Random().nextInt(tips.length);
                tipTextView.setText(tips[randomIndex]);
                // 每3秒执行一次
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(runnable); // 初始化延迟，立即开始
    }
    public void stopUpdatingTip() {
        if (runnable != null && handler != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
