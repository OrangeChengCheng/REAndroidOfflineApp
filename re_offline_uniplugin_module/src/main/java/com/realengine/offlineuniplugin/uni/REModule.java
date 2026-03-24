package com.realengine.offlineuniplugin.uni;

import static io.dcloud.feature.uniapp.common.TypeUniModuleFactory.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.engine.REEngineActivity;
import com.realengine.offlineuniplugin.reclass.FileInfo;
import com.realengine.offlineuniplugin.retool.DBUtil;
import com.realengine.offlineuniplugin.retool.FileManager;
import com.realengine.offlineuniplugin.retool.FilePickerHelper;
import com.realengine.offlineuniplugin.retool.StoragePermissionHelper;
import com.realengine.offlineuniplugin.retool.ZipOperator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

import BlackHole3D.REDVec3;
import BlackHole3D.REDVec4;
import BlackHole3D.REDataSet;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class REModule extends UniModule {
    private static final String TAG = "REModule";
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
    private FilePickerHelper.OnFileSelectedListener fileSelectListener;

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
                // 获取存储权限
                StoragePermissionHelper.hasStoragePermission(((Activity)mUniSDKInstance.getContext()));

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
        String msg = options.getString("msg");
        Log.d(TAG, "************* 【uni -> app】 : (unipluginLog)   " + msg);
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


    @UniJSMethod(uiThread = false)
    public void saveUniFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (saveUniFile)   " + options);
        String uniDownloadTempPath = options.getString("uniDownloadTempPath");

        boolean isSuccess = FileManager.copyUniFile(mUniSDKInstance.getContext(), uniDownloadTempPath);

        JSONObject result = new JSONObject();
        result.put("success", isSuccess);
        result.put("uniDownloadTempPath", uniDownloadTempPath);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }

    @UniJSMethod(uiThread = false)
    public void getLocFileList(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (getLocFileList)   " + options);

        FileManager fileManager = new FileManager(mUniSDKInstance.getContext());
        List<FileInfo> fileList = fileManager.getLocFileList("");
//        List<FileInfo> fileList = fileManager.getLocFileList("BlackHole Engine SDK_v3.2.0.3559");
//        List<FileInfo> fileList = fileManager.getLocFileList("BlackHole Engine SDK_v3.2.0.3559/BlackHole Engine SDK_v3.2.0.3559");


        JSONObject result = new JSONObject();
        result.put("success", fileList.size());
        result.put("fileList", fileList);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }

    @UniJSMethod(uiThread = false)
    public void unzipFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (unzipFile)   " + options);
        String filePath = options.getString("filePath");
        String fileName = options.getString("fileName");

        ZipOperator zipOperator = new ZipOperator(mUniSDKInstance.getContext());

//        JSONObject zipInfo = zipOperator.readZipCommentsOnly(fileName, "@Yr5!Uk9$Bn3*Lp6#Qj8&Mx2");
///storage/emulated/0/Android/data/com.realengine.androidofflineapp/files/REOfflineDoc/[model]药店-BIM案例模型.rvt

        String zipFinishFilePath = zipOperator.unzipFile(fileName, "@Yr5!Uk9$Bn3*Lp6#Qj8&Mx2");

        FileManager fileManager = new FileManager(mUniSDKInstance.getContext());
        List<FileInfo> fileList = fileManager.getFileList(zipFinishFilePath);

        JSONObject result = new JSONObject();
        result.put("success", zipFinishFilePath.length());
        result.put("fileList", fileList);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }


    @UniJSMethod(uiThread = false)
    public void delFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (delFile)   " + options);

        FileManager fileManager = new FileManager(mUniSDKInstance.getContext());

        boolean isSuccess = FileManager.deleteAllFiles(fileManager.getAppLocFile(""), true);

        List<FileInfo> fileList = fileManager.getLocFileList("");
        JSONObject result = new JSONObject();
        result.put("success", isSuccess);
        result.put("fileList", fileList);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }

    @UniJSMethod(uiThread = false)
    public void selFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (selFile)   " + options);

        Context context = mUniSDKInstance.getContext();
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity");
            // 可以回调给 JS 层错误
            return;
        }
        final Activity activity = (Activity) context;

        // 设置回调监听
        fileSelectListener = new FilePickerHelper.OnFileSelectedListener() {
            @Override
            public void onSuccess(FilePickerHelper.PickerFile file) {
                Log.d(TAG, "选择成功: " + file.toString());
                // 在这里通过 JS 回调返回结果给前端
                // 例如：mUniSDKInstance.fireEvent(...) 或使用 callback
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "选择失败: " + error);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "用户取消选择");
            }
        };

        // 在 UI 线程启动选择器
        activity.runOnUiThread(() -> {
            FilePickerHelper.startPickSingleFile(activity, new String[]{"*/*"});
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FilePickerHelper.REQUEST_CODE_PICK_SINGLE_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                FilePickerHelper.PickerFile file = FilePickerHelper.getFileFromResult(mUniSDKInstance.getContext(), data);
                if (file != null && fileSelectListener != null) {
                    fileSelectListener.onSuccess(file);
                } else if (fileSelectListener != null) {
                    fileSelectListener.onError("无法获取文件信息");
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (fileSelectListener != null) fileSelectListener.onCancel();
            }
            fileSelectListener = null; // 清空监听
        }
    }



    @UniJSMethod(uiThread = false)
    public void useFileUniToApp(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (useFileUniToApp)   " + options);
        String type = options.containsKey("type") ? String.valueOf(options.get("type")) : "";
        JSONObject json_data = options.containsKey("data") ? options.getObject("data", options.getClass()) : null;

        JSONObject result = new JSONObject();
        FileManager fileManager = new FileManager(mUniSDKInstance.getContext());
        if (type.equals("delFile")) {
            String filePath = json_data.containsKey("filePath") ? String.valueOf(json_data.get("filePath")) : "";
            boolean isSuccess = FileManager.deleteAllFiles(filePath, true);
            result.put("success", isSuccess);

        } else if (type.equals("getAllSubFileList")) {
            String filePath = json_data.containsKey("filePath") ? String.valueOf(json_data.get("filePath")) : "";
            List<FileInfo> fileList = fileManager.getFileSubList(filePath);
            result.put("success", fileList.size() > 0 ? true : false);
            result.put("data", fileList);
        }

        if(callback != null) {
            callback.invoke(result);
        }
    }


    @UniJSMethod(uiThread = false)
    public void showOfflineEngine(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (showOfflineEngine)   " + options);

        String jsonStr = JSONObject.toJSONString(options);

//        String fileName = options.getString("fileName");
//        String filePath = options.getString("filePath");
//        Integer type = options.getInteger("type");
//
//        FileManager fileManager = new FileManager(mUniSDKInstance.getContext());
//        List<FileInfo> fileList = fileManager.getFileSubList(filePath);
//
//        if (fileList.isEmpty()) return;
//
//        List<REDataSet> arrDataSet =new ArrayList<>();
//        if (type == 0) {
//            BlackHole3D.REDataSet dataSet = new BlackHole3D.REDataSet();
//            dataSet.dataSetId = fileName;
//            dataSet.resourcesAddress = filePath + "/res";
//            arrDataSet.add(dataSet);
//        } else  {
//            List<String> resFolderNameList = FileManager.getSubFolderNames(filePath + "/res");
//            for (String resFolderName : resFolderNameList) {
//                BlackHole3D.REDataSet dataSet = new BlackHole3D.REDataSet();
//                dataSet.dataSetId = resFolderName;
//                dataSet.resourcesAddress = filePath + "/res/" + resFolderName;
//                arrDataSet.add(dataSet);
//            }
//        }
//
//        JSONObject data = new JSONObject();
//        data.put("data", arrDataSet);
//        String jsonStr = JSON.toJSONString(data);

        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            try {
                Intent intent = new Intent(mUniSDKInstance.getContext(), REEngineActivity.class);
                intent.putExtra("intent_obj", jsonStr);

                ((Activity)mUniSDKInstance.getContext()).startActivity(intent);

            } catch (Exception e) {
                Log.e("native page error", e.getMessage());
            }
        }
    }


    @UniJSMethod(uiThread = false)
    public void dbQuery(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (dbQuery)   " + options);

        String dbPath = options.getString("dbPath");
        String sql = options.getString("sql");

//        ZipOperator zipOperator = new ZipOperator(mUniSDKInstance.getContext());
//        JSONObject zipInfo = zipOperator.readZipCommentsOnly(fileName + ".zip", "@Yr5!Uk9$Bn3*Lp6#Qj8&Mx2");
//        String Id = zipInfo.getObject("globalComment", JSONObject.class).getString("Id");

        List<Map<String, Object>> data = DBUtil.queryToList(dbPath, sql, null);

        JSONObject result = new JSONObject();
        result.put("success", data.isEmpty() ? false : true);
        result.put("data", data);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }

    @UniJSMethod(uiThread = false)
    public void dbTableExist(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (dbTableExist)   " + options);
        String dbPath = options.getString("dbPath");
        String tableName = options.getString("tableName");

        boolean isExist = DBUtil.isTableExists(dbPath, tableName);
        JSONObject result = new JSONObject();
        result.put("data", isExist);
        // 回调 Uni 端（UniApp 标准回调方式）
        if(callback != null) {
            callback.invoke(result);
        }
    }
}
