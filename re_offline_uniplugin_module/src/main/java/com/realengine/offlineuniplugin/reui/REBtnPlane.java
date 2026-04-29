package com.realengine.offlineuniplugin.reui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.realengine.offlineuniplugin.R;
import com.realengine.offlineuniplugin.retool.REDeviceUtils;


public class REBtnPlane {
    private Context context;
    private ViewGroup main_layout;
    private View re_btnPlane;
    private LinearLayout reBtnPlanTree;
    private LinearLayout reBtnPlanProperty;
    private Button reBtnTree;
    private Button reBtnProperty;
    private boolean isTreeSelected = false;
    private boolean isPropertySelected = false;
    private BtnCallback btnCallback;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());


    public interface BtnCallback {
        void onClickCallback(String btnId, boolean isSelected);
    }


    public REBtnPlane(Context context, ViewGroup main_layout, BtnCallback btnCallback) {
        this.context = context;
        this.main_layout = main_layout;
        this.btnCallback = btnCallback;

        re_btnPlane = LayoutInflater.from(context).inflate(R.layout.re_btn_plane_layout, main_layout, false);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topMargin = REDeviceUtils.dpToPx(context, 44 + 27);
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.leftMargin = REDeviceUtils.dpToPx(context, 12);
        re_btnPlane.setLayoutParams(params);

        // 初始化控件
        initViews();
        // 设置点击事件
        setClickListeners();

        this.main_layout.addView(re_btnPlane);
    }

    private void initViews() {
        reBtnPlanTree = re_btnPlane.findViewById(R.id.re_btn_plan_tree);
        reBtnPlanProperty = re_btnPlane.findViewById(R.id.re_btn_plan_property);
        reBtnTree = re_btnPlane.findViewById(R.id.re_btn_tree);
        reBtnProperty = re_btnPlane.findViewById(R.id.re_btn_property);
    }

    private void setClickListeners() {
        // 树按钮点击事件：选中树按钮时，自动取消属性按钮
        reBtnTree.setOnClickListener(view -> {
            if (isTreeSelected) {
                // 当前已选中，点击则取消
                isTreeSelected = false;
            } else {
                // 当前未选中，点击则选中，并取消另一个
                isTreeSelected = true;
                if (isPropertySelected) {
                    isPropertySelected = false; // 互斥：取消属性按钮
                    updatePropertyButtonState(true); // 同步更新属性按钮状态
                }
            }
            // 更新当前按钮状态
            updateTreeButtonState(true);
        });

        // 属性按钮点击事件：选中属性按钮时，自动取消树按钮
        reBtnProperty.setOnClickListener(view -> {
            if (isPropertySelected) {
                // 当前已选中，点击则取消
                isPropertySelected = false;
            } else {
                // 当前未选中，点击则选中，并取消另一个
                isPropertySelected = true;
                if (isTreeSelected) {
                    isTreeSelected = false; // 互斥：取消树按钮
                    updateTreeButtonState(true); // 同步更新树按钮状态
                }
            }
            // 更新当前按钮状态
            updatePropertyButtonState(true);
        });
    }

    public void updataState(String btnId, boolean isSelected) {
        if (btnId.equals("web_pop_tree")) {
            isTreeSelected = isSelected;
            updateTreeButtonState(false);
        } else if (btnId.equals("web_pop_property")) {
            isPropertySelected = isSelected;
            updatePropertyButtonState(false);
        }
    }

    // 更新树按钮状态
    private void updateTreeButtonState(boolean isMsg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isTreeSelected) {
                    // 选中状态：改变背景色和图片
                    reBtnPlanTree.setBackgroundResource(R.drawable.btn_plane_check_bg);
                    reBtnTree.setSelected(true);
                } else {
                    // 未选中状态：恢复默认
                    reBtnPlanTree.setBackgroundResource(R.drawable.btn_plane_normal_bg);
                    reBtnTree.setSelected(false);
                }
            }
        });

        if (isMsg && btnCallback != null) {
            btnCallback.onClickCallback("web_pop_tree", isTreeSelected);
        }
    }

    // 更新属性按钮状态
    private void updatePropertyButtonState(boolean isMsg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isPropertySelected) {
                    // 选中状态：改变背景色和图片
                    reBtnPlanProperty.setBackgroundResource(R.drawable.btn_plane_check_bg);
                    reBtnProperty.setSelected(true);
                } else {
                    // 未选中状态：恢复默认
                    reBtnPlanProperty.setBackgroundResource(R.drawable.btn_plane_normal_bg);
                    reBtnProperty.setSelected(false);
                }
            }
        });

        if (isMsg && btnCallback != null) {
            btnCallback.onClickCallback("web_pop_property", isPropertySelected);
        }
    }
}