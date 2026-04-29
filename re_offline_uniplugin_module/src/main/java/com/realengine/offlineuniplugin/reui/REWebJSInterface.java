package com.realengine.offlineuniplugin.reui;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.realengine.offlineuniplugin.reclass.REUnifiedTypeSerializer;

import BlackHole3D.REBBox3D;
import BlackHole3D.REDVec2;
import BlackHole3D.REDVec3;
import BlackHole3D.REDVec4;
import BlackHole3D.REIVec2;



public class REWebJSInterface {
    private Context context;
    private WebView webView;
    private Callback callback;

    /**
     * 回调接口，用于将 Android 数据传递给 Vue
     */
    public interface Callback {
        void onMessageReceived(String message);
    }

    /**
     * 初始化数据处理接口
     */
    public REWebJSInterface(Context context, WebView webView, Callback callback) {
        this.context = context;
        this.webView = webView;
        this.callback = callback;
        registerSerializers();
    }

    /**
     * 注册序列化器（无需注解）
     */
    private void registerSerializers() {
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        REUnifiedTypeSerializer serializer = new REUnifiedTypeSerializer();

        serializeConfig.put(REBBox3D.class, serializer);
        serializeConfig.put(REDVec4.class, serializer);
        serializeConfig.put(REDVec3.class, serializer);
        serializeConfig.put(REDVec2.class, serializer);
        serializeConfig.put(REIVec2.class, serializer);
    }


    /**
     * 暴露给 JavaScript 的方法，用于接收 Vue 发送的消息
     * 接收 JavaScript 消息
     */
    @JavascriptInterface
    public void sendMsgWebToApp(String jsonMessage) {
        Log.d("WebView", "收到 Vue 消息: " + jsonMessage);
        try {
            if (callback != null) {
                callback.onMessageReceived(jsonMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onMessageReceived("JSON 解析失败: " + e.getMessage());
            }
        }
    }



    /**
     * 发送消息到 JavaScript
     */
    public void sendMsgAppToWeb(String message) {
        if (webView == null) return;

        webView.post(() -> {
            String script = "javascript:window.REWebApp.onAppToWebMessage('" + message + "')";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(script, null);
            } else {
                webView.loadUrl(script);
            }
        });
    }

    /**
     * 发送对象到 JavaScript（自动序列化为 JSON）
     */
    public void sendObjAppToWeb(Object object, String type, boolean isResponse, String msgId) {
        if (webView == null) return;
        // 组合多个对象
        JSONObject result = new JSONObject();
        result.put("data", object);
        result.put("type", type);
        result.put("isResponse", isResponse);
        result.put("msgId", msgId);

        String jsonString = JSON.toJSONString(result,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.DisableCircularReferenceDetect);


        if (jsonString.isEmpty()) {
            // 序列化失败
            return;
        }
        // 转义符修复
        String safeJsonString = fixFastJsonEscape(jsonString);

        sendMsgAppToWeb(safeJsonString);
    }

    /**
     * 发送对象到 JavaScript（自动序列化为 JSON）
     */
    public void sendObjAppToWeb(Object object, String type) {
        sendObjAppToWeb(object, type, false, "");
    }

    /**
     * 发送对象到 JavaScript（自动序列化为 JSON）
     */
    public void sendObjAppToWeb(Object object) {
        sendObjAppToWeb(object, "", false, "");
    }

    /**
     * 发送对象到 JavaScript（自动序列化为 JSON）
     */
    public void sendObjAppToWeb(String type) {
        sendObjAppToWeb(new JSONObject(), type, false, "");
    }

    /**
     * 发送对象到 JavaScript（自动序列化为 JSON）
     * 含有回调信息
     */
    public void sendObjAppToWebCallback(Object object, String msgId) {
        sendObjAppToWeb(object, "", true, msgId);
    }


//    /**
//     * 处理非法数据
//     */
//    private String escapeJson(String json) {
//        return json.replace("\\", "\\\\")
//                .replace("\"", "\\\"")
//                .replace("\n", "\\n")
//                .replace("\r", "\\r")
//                .replace("\t", "\\t");
//    }



    /**
     * 核心：修复FastJSON序列化后的转义符
     * 解决JS解析时的"invalid escape in identifier"错误
     */
    private String fixFastJsonEscape(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return "";

        // 关键：将 \ 转为 \\
        String fixed = jsonString.replace("\\", "\\\\");
        // 补充：换行符 " → \"
        fixed = fixed.replace("\"", "\\\"");
        // 补充：换行符 \n → \\n
        fixed = fixed.replace("\n", "\\n");
        // 补充：回车符 \r → \\r
        fixed = fixed.replace("\r", "\\r");
        // 可选：制表符 \t → \\t
        fixed = fixed.replace("\t", "\\t");

        return fixed;
    }


}