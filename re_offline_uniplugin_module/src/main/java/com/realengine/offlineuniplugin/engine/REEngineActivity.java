package com.realengine.offlineuniplugin.engine;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.realengine.offlineuniplugin.R;

import BlackHole3D.RealEngineActivity;

public class REEngineActivity extends RealEngineActivity {
    //添加引擎回调监听
    public REListener reListener = new REListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //注册引擎监听
        BlackHole3D.System.AddEventListener(reListener);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reengine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //注册渲染界面
        mSurface = new BlackHole3D.RealEngineSurface(getApplication());
        mLayout =findViewById(R.id.re_win);
        if (mLayout!=null){
            mLayout.addView(mSurface);
        }

        setWindowStyle(true);//true为全屏，否则有电量\时间等
    }
}
