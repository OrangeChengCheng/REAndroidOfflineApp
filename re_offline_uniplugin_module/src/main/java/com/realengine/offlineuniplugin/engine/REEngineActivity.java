package com.realengine.offlineuniplugin.engine;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.realengine.offlineuniplugin.R;
import com.realengine.offlineuniplugin.reclass.REDataSetUniData;
import com.realengine.offlineuniplugin.reclass.RESceneUniData;
import com.realengine.offlineuniplugin.retool.StoragePermissionHelper;
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
    private RESceneUniData sceneUniData;

    //添加引擎回调监听
    public REListener reListener = new REListener(this, new REListenerHandle() {
        @Override
        public void engineCreated(int procRet) {

            loadBim();
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

    public void loadBim() {
        BlackHole3D.System.setViewMode(REVpTypeEm.BIM,REVpTypeEm.None,REVpRankEm.Single);
        List<REDataSet> arrDataSet =new ArrayList<>();
        for (REDataSetUniData dataSetInfo : sceneUniData.dataSetList) {
            REDataSet dataSet = new REDataSet();
            dataSet.setDataSetId(dataSetInfo.dataSetId);
            dataSet.setResourcesAddress(dataSetInfo.resourcesAddress);
            dataSet.setScale(new REDVec3(dataSetInfo.scale.get(0), dataSetInfo.scale.get(1), dataSetInfo.scale.get(2)));
            dataSet.setRotate(new REDVec4(dataSetInfo.rotate.get(0), dataSetInfo.rotate.get(1), dataSetInfo.rotate.get(2), dataSetInfo.rotate.get(3)));
            dataSet.setOffset(new REDVec3(dataSetInfo.offset.get(0), dataSetInfo.offset.get(1), dataSetInfo.offset.get(2)));
            dataSet.setDataSetCRS(dataSetInfo.dataSetCRS);
            dataSet.setDataSetCRSNorth(dataSetInfo.dataSetCRSNorth);
            dataSet.setEngineOrigin(new REDVec3(dataSetInfo.engineOrigin.get(0), dataSetInfo.engineOrigin.get(1), dataSetInfo.engineOrigin.get(2)));
            dataSet.setDataSetSGContent(dataSetInfo.dataSetSGContent);
            arrDataSet.add(dataSet);
        }
        BlackHole3D.Model.loadDataSet(arrDataSet,true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        accessPermission();
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

    private void accessPermission() {
        // 获取存储权限
        StoragePermissionHelper.hasStoragePermission(this);
//        if (Build.VERSION.SDK_INT >= 30) {
//            if (!Environment.isExternalStorageManager()) {
//                Intent intent = new Intent();
//                // 使用正确的 Action: ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//
//                // 关键步骤：设置 Data 为 "package:你的应用包名"
//                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
//                intent.setData(uri);
//
//                // 启动 Intent
//                startActivity(intent);
//            }
//        }
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

        if (jsonObject != null) {
            sceneUniData = JSON.parseObject(jsonObject.toJSONString(), new TypeReference<RESceneUniData>(){});
        }
//        // 将JSON字符串解析为JSONObject
//        JSONObject jsonObject = JSON.parseObject(intent_obj);
//        JSONArray dataArray = jsonObject.getJSONArray("data");
//        dataSetList = dataArray.toJavaList(REDataSet.class);



        int i = 0;
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
