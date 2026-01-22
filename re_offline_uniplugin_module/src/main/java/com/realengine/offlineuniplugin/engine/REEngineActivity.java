package com.realengine.offlineuniplugin.engine;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.realengine.offlineuniplugin.R;
import com.realengine.offlineuniplugin.reui.RELoading;
import com.realengine.offlineuniplugin.reui.RENav;
import com.realengine.offlineuniplugin.reui.RENavHandle;
import com.realengine.offlineuniplugin.uni.REModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import BlackHole3D.*;

public class REEngineActivity extends RealEngineActivity {
    static String TAG = "REEngineActivity";
    private RELoading re_loading;
    private RENav re_nav;

    //添加引擎回调监听
    public REListener reListener = new REListener(this, new REListenerHandle() {
        @Override
        public void engineCreated(int procRet) {
            List<REDataSet> arrDataSet =new ArrayList<>();
            BlackHole3D.REDataSet dataSet = new BlackHole3D.REDataSet();
            dataSet.setDataSetId("dataSet01");
            dataSet.setResourcesAddress("https://demo.bjblackhole.com/default.aspx?dir=url_res03&path=res_jifang");
            arrDataSet.add(dataSet);
            BlackHole3D.Model.loadDataSet(arrDataSet,true);
        }
        @Override
        public void loadDataSetFinish(int procRet) {
            if (procRet == 1) {
                Log.d("RealEngine","" + "REDataSetLoadFinish----------"+String.valueOf(procRet));
                re_loading.hide();
            } else {
                REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T2, "模型资源加载失败！");
                cancelLoadAction(null);
            }
        }
        @Override
        public void loadCADFinish(int procRet) {
            if (procRet == 1) {
                Log.d("RealEngine","" + "loadCADFinish----------"+String.valueOf(procRet));
                re_loading.hide();
            } else {
                REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T2, "模型资源加载失败！");
                cancelLoadAction(null);
            }
        }
        @Override
        public void loadProgress(int progress, String str) { re_loading.updateProgress(progress); }
        @Override
        public void systemUIEvent(String btnName, int btnState) { }
        @Override
        public void systemSelElement(boolean success) { }
        @Override
        public void systemSelShpElement(boolean success) { }
    });

    public void cancelLoadAction(View view) {
//        if (sceneUniData.shareType == 1 && sceneUniData.shareDataType.equals("Cad")) {
//            BlackHole3D.CAD.unloadCAD();
//        } else {
            BlackHole3D.Model.unloadAllDataSet();
//        }
        BlackHole3D.System.releaseEngine();
        re_loading.stopUpdatingTip();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //处理数据
        handleData();
        //注册引擎监听
        registerUniListener();
        //加载主界面要在注册监听之后
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //注册引擎渲染界面
        initEngine();
        //初始化导航栏
        initNav();
        //初始化RELoading
        re_loading = new RELoading(this, findViewById(R.id.main_layout));
        initUI();
    }


    private void initUI() {
        // 设置状态栏颜色与导航栏一致（#0368ff）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#000000"));

            // 沉浸式设置（让布局延伸到状态栏）
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  );
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
    private void initEngine() {
        mSurface = new BlackHole3D.RealEngineSurface(getApplication());
        mLayout =findViewById(R.id.re_win);
        if (mLayout!=null){
            mLayout.addView(mSurface);
        }
    }
    private void handleData() {
        String intent_obj = getIntent().getStringExtra("intent_obj");
        // 将JSON字符串解析为JSONObject
        JSONObject jsonObject = JSON.parseObject(intent_obj);
    }
    private void registerUniListener() {
        BlackHole3D.System.AddEventListener(reListener);
        REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T1, "Engine Activity onCreate");
        REModule.registerUniToAppMsg(jsonObject -> {
            handleEngineSDK(jsonObject, 1);
        });
    }
    private void initNav() {
        re_nav = new RENav(this, findViewById(R.id.main_layout), "黑洞引擎", new RENavHandle() {
            @Override
            public void navBackCallBack() {
                Log.d(TAG,"" + "navBackCallBack----------");
                cancelLoadAction(null);
            }
            @Override
            public void navScanCallBack() { }
        });

    }
    private void handleEngineSDK (JSONObject jsonObject, int msgWhere) {
        String msgId = jsonObject.containsKey("msgId") ? String.valueOf(jsonObject.get("msgId")) : "";
        String webPopId = jsonObject.containsKey("webPopId") ? String.valueOf(jsonObject.get("webPopId")) : "";
        String type = jsonObject.containsKey("type") ? String.valueOf(jsonObject.get("type")) : "";
        JSONObject json_data = jsonObject.containsKey("data") ? jsonObject.getObject("data", jsonObject.getClass()) : null;
        if (json_data == null) return;

    }

}
