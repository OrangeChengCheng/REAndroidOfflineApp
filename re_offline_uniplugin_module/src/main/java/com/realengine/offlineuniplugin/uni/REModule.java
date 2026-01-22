package com.realengine.offlineuniplugin.uni;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.engine.REEngineActivity;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class REModule extends UniModule {

    @UniJSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, UniJSCallback callback) {
        Log.e("REModule", "testAsyncFunc--"+options);
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            try {
                Intent intent = new Intent(mUniSDKInstance.getContext(), REEngineActivity.class);
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
}
