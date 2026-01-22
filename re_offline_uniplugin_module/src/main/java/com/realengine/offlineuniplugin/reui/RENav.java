package com.realengine.offlineuniplugin.reui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.realengine.offlineuniplugin.R;


public class RENav {
    private RENavHandle nav_handle;
    private Context context;
    private ViewGroup main_layout;
    private View re_nav;
    private LinearLayout re_nav_btn_back;
    private ImageView re_nav_iv_scan;
    private TextView re_nav_title;

    public RENav(Context context, ViewGroup main_layout, String title, RENavHandle nav_handle) {
        this.context = context;
        this.main_layout = main_layout;
        re_nav = LayoutInflater.from(context).inflate(R.layout.re_nav_layout, main_layout, false);
        re_nav_title = re_nav.findViewById(R.id.re_nav_title);
        re_nav_title.setText(title);
        re_nav_btn_back = re_nav.findViewById(R.id.re_nav_back);
        re_nav_btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        nav_handle.navBackCallBack();
                    }
                });
            }
        });
        re_nav_iv_scan = re_nav.findViewById(R.id.re_nav_scan);
        re_nav_iv_scan.setVisibility(View.GONE);
        re_nav_iv_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        nav_handle.navScanCallBack();
                    }
                });
            }
        });
        this.main_layout.addView(re_nav);
    }



}


