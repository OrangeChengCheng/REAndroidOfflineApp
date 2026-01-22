package com.realengine.offlineuniplugin.uni;

import static io.dcloud.feature.uniapp.common.TypeUniModuleFactory.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.engine.REEngineActivity;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class REModule extends UniModule {

    public enum REModule_CallBackEm {
        REModule_T1(1),// 发送事件状态信息
        REModule_T2(2),// 发送数据信息
        REModule_CheckboxCallback(3),// checkbox点击
        REModule_GetCamLoc(4);// 获取相机信息
        private int value;
        private REModule_CallBackEm(int value) {
            this.value = value;
        }
        public int value() {
            return this.value;
        }
        public static REModule_CallBackEm fromInt(int value) {
            for (REModule_CallBackEm e : REModule_CallBackEm.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }

    public static UniJSCallback appToUniCallBack;
    private static REModuleCallback uniToAppCallBack;

    private static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what > 0) {
                handleAppToUniMsg(msg);
            } else {
                super.handleMessage(msg);
            }
        }
    };

    // 回调接口定义，增加传递响应数据的功能
    public interface REModuleCallback {
        void onTaskComplete(JSONObject jsonObject);
    }

    public static void registerUniToAppMsg(REModuleCallback callback) {
        REModule.uniToAppCallBack = callback;
    }

    public static void sendMsgAppToUni(REModule_CallBackEm what, Object obj) {
        if (what == null) {
            Log.e(TAG, "sendMessage: Invalid callback type");
            return;
        }
        Message message = handler.obtainMessage();
        message.what = what.value();
        message.obj = obj;
        handler.sendMessage(message);
    }

    @UniJSMethod(uiThread = true)
    public void realEngineRender(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (realEngineRender)  " + options);

        String jsonStr = JSONObject.toJSONString(options);

        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            try {
                Intent intent = new Intent(mUniSDKInstance.getContext(), REEngineActivity.class);
                intent.putExtra("intent_obj", jsonStr);

                ((Activity)mUniSDKInstance.getContext()).startActivity(intent);

            } catch (Exception e) {
                Log.e("native page error", e.getMessage());
            }
        }
        if(callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            callback.invoke(data);
        }
    }

    @UniJSMethod(uiThread = true)
    public void unipluginLog(JSONObject options) {
        Log.d(TAG, "************* 【uni -> app】 : (unipluginLog)   " + options);
    }

    @UniJSMethod (uiThread = false)
    public void registerAppMsg(UniJSCallback callback) {
        this.appToUniCallBack = callback;
    }

    @UniJSMethod (uiThread = false)
    public void sendMsgUniToApp(JSONObject options) {
        Log.d(TAG, "************* 【uni -> app】 : (sendMsgUniToApp)   " + options);
        if (uniToAppCallBack != null) {
            uniToAppCallBack.onTaskComplete(options);
        }
    }

    private static void handleAppToUniMsg(Message msg) {
        REModule_CallBackEm callbackEm = REModule_CallBackEm.fromInt(msg.what);
        switch (callbackEm) {
            case REModule_T1:
            {
                if (appToUniCallBack != null) {
                    Log.d(TAG, "************* 【app -> uni】 :  " + msg.obj);
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    appToUniCallBack.invokeAndKeepAlive(data);
                }
            }
            break;
            case REModule_T2:
            {
                // 加载报错
                if (appToUniCallBack != null) {
                    Log.d(TAG, "************* 【app -> uni】 :  " + msg.obj);
//                    String jsonStr = JSONObject.toJSONString(msg.obj);
                    JSONObject data = new JSONObject();
                    data.put("code", "error");
                    data.put("msg", msg.obj);
                    appToUniCallBack.invokeAndKeepAlive(data);
                }
            }
            break;
            case REModule_CheckboxCallback:
            {
                sendToUniapp(msg.obj, "checkboxCallback");
            }
            break;
            case REModule_GetCamLoc:
            {
                sendToUniapp(msg.obj, "Camera.getCamLocate");
            }
            break;
        }

    }

    private static void sendToUniapp(Object data, String type) {
        if (appToUniCallBack != null) {
            JSONObject dataToUniapp = new JSONObject();
            dataToUniapp.put("data", data);
            dataToUniapp.put("type", type);
            Log.d(TAG, "************* 【app -> uni】 :  " + dataToUniapp);
            appToUniCallBack.invokeAndKeepAlive(dataToUniapp);
        }
    }

}
