package com.realengine.offlineuniplugin.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import BlackHole3D.REDVec2;
import BlackHole3D.REDataSet;
import BlackHole3D.RealEngineIListener;

public class REListener implements RealEngineIListener {
    private REListenerHandle listenerHandle;
    private Context context;

    public REListener(Context context, REListenerHandle listenerHandle) {
        this.listenerHandle = listenerHandle;
        this.context = context;
    }

    @Override
    public void RESystemEngineCreated(int procRet) {
        //系统初始化是否完成，当完成是procRet=1；例如：RealBIMInitSysReady---------1
        Log.d("RealEngine","" + "RESystemEngineCreated---------"+String.valueOf(procRet));
        Log.d("RealEngine", "BlackHole3D_AndroidSDK_Version: " + BlackHole3D.System.getVersion());

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.engineCreated(procRet);
            }
        });
    }

    @Override
    public void REDataSetLoadFinish(int procRet) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.loadDataSetFinish(procRet);
            }
        });
    }

    @Override
    public void REDataSetLoadProgress(int progress, String str) {
        //引擎的加载进度1-100；
        Log.d("RealEngine","" + "REDataSetLoadProgress:Progress:"+String.valueOf(progress)+"*****"+str);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.loadProgress(progress, str);
            }
        });
    }

    @Override
    public void RESystemSelElement(boolean b) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.systemSelElement(b);
            }
        });
    }

    @Override
    public void RESystemSelShpElement(boolean b) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.systemSelShpElement(b);
            }
        });
    }

    @Override
    public void RELocateCam(int i) {
        //相机运动完成事件
    }

    @Override
    public void RECameraMove() {
        //相机运动的监听事件
    }

    @Override
    public void REClipFinish(int i) {
        //裁剪完成回调事件
    }

    @Override
    public void REDataSetLoadPanFinish(int i) {
        //全景场景加载完成事件
        //注：监听到此事件并不会直接在图形窗口显示全景图，需要调用setViewMode接口设置窗口显示模式才可以
    }

    @Override
    public void REPanLoadSingleFinish(int i) {
        //全景场景中某一帧全景图设置成功的事件
    }

    @Override
    public void REPanSelShpElement(boolean b) {
        //全景场景鼠标拾取事件，鼠标点击全景场景后会触发该事件
        //注：可通过getCurShpProbeRet接口获取拾取信息
    }

    @Override
    public void RECADLoadFinish(int procRet) {
        //二维图纸加载完成事件，图纸加载完成后会触发该事件
        //注：该事件调用完成才可以调用图纸相关的其他接口
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.loadCADFinish(procRet);
            }
        });
    }

    @Override
    public void RECADSelElement(String s, String s1, REDVec2 redVec2) {
        //二维图元点击事件，点击某一个二维图元，会触发该事件
    }

    @Override
    public void RECADSelAnchor(String s) {
        //锚点点击事件，点击某一个锚点，会触发该事件
    }

    @Override
    public void RECADSelShpAnchor(String s) {
        //自定义锚点点击事件，点击某一个自定义锚点，会触发该事件
    }

    @Override
    public void REMiniMapLoadCAD(int i) {
        //在小地图中二维图纸加载完成事件，在小地图中图纸加载完成后会触发该事件
    }

    @Override
    public void REMiniMapCADSelShpAnchor(String s) {
        //小地图中CAD锚点点击事件，点击某一个锚点，会触发该事件
    }

    @Override
    public void REElevationUpdateFinish(int i) {
        //标高数据更新完成回调事件
        //注：相机调整会触发标高数据更新
    }

    @Override
    public void REAxisGridUpdateFinish(int i) {
        //轴网数据更新完成回调事件
    }

    @Override
    public void REAddEntityFinish(int i) {
        //单构件添加完成回调事件
    }

    @Override
    public void RESystemUIEvent(String btnName, int btnState) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                listenerHandle.systemUIEvent(btnName, btnState);
            }
        });
    }
}
