package com.realengine.offlineuniplugin.uni;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.engine.REEngineActivity;
import com.realengine.offlineuniplugin.reclass.FileInfo;
import com.realengine.offlineuniplugin.retool.DBUtil;
import com.realengine.offlineuniplugin.retool.FileUtil;
import com.realengine.offlineuniplugin.retool.FilePickerHelper;
import com.realengine.offlineuniplugin.retool.StoragePermissionHelper;
import com.realengine.offlineuniplugin.retool.ZipUtil;

import java.util.List;
import java.util.Map;

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





    /** ------------ 引擎操作 -----------*/
    // 引擎查看本地资源
    @UniJSMethod(uiThread = true)
    public void showOfflineEngine(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (showOfflineEngine)   " + options);

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
    }



    /** ------------ 文件操作 -----------*/
    // 获取指定文件夹下所有文件列表
    @UniJSMethod(uiThread = false)
    public void fileGetAllChild(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileGetAllChild)   " + options);
        String filePath = options.getString("filePath");

        List<FileInfo> fileList = FileUtil.getAllChild(filePath);

        JSONObject result = new JSONObject();
        result.put("success", fileList.size() > 0 ? true : false);
        result.put("data", fileList);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 递归删除指定路径下的所有内容
    @UniJSMethod(uiThread = false)
    public void fileDelAllSubFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileDelAllSubFile)   " + options);
        String filePath = options.getString("filePath");
        boolean keepDir = options.getBoolean("keepDir");

        boolean isSuccess = FileUtil.deleteAllFiles(filePath, keepDir);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("data", isSuccess);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 判断文件是否存在
    @UniJSMethod(uiThread = false)
    public void fileExist(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileExist)   " + options);
        String filePath = options.getString("filePath");

        boolean isExist = FileUtil.fileExist(filePath);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("data", isExist);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 获取文件父级
    @UniJSMethod(uiThread = false)
    public void fileGetParent(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileGetParent)   " + options);
        String filePath = options.getString("filePath");

        FileInfo parent = FileUtil.getParentFile(filePath);

        JSONObject result = new JSONObject();
        result.put("success", parent != null);
        result.put("data", parent);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 获取文件夹内指定后缀的文件列表
    @UniJSMethod(uiThread = false)
    public void fileGetChildBySuffix(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileGetChildBySuffix)   " + options);
        String filePath = options.getString("filePath");
        String suffix = options.getString("suffix");

        List<FileInfo> fileList = FileUtil.getChildFilesBySuffix(filePath, suffix);

        JSONObject result = new JSONObject();
        result.put("success", fileList.size() > 0 ? true : false);
        result.put("data", fileList);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 获取沙盒默认存储路径（外部存储）
    @UniJSMethod(uiThread = false)
    public void fileGetAppRootFolder(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileGetAppResFolder)   " + options);
        FileInfo folder = FileUtil.getAppRootFolder(mUniSDKInstance.getContext());

        JSONObject result = new JSONObject();
        result.put("success", folder != null);
        result.put("data", folder);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 拷贝单个文件（外部存储）
    @UniJSMethod(uiThread = false)
    public void fileCopyFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileCopyFile)   " + options);
        String sourceFilePath = options.getString("sourceFilePath");
        String targetFilePath = options.getString("targetFilePath");

        boolean isSuccess = FileUtil.copyFile(sourceFilePath, targetFilePath);

        JSONObject result = new JSONObject();
        result.put("success", isSuccess);
        result.put("data", isSuccess);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 创建文件夹（外部存储）
    @UniJSMethod(uiThread = false)
    public void fileCreateFolder(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileCreateFolder)   " + options);
        String folderPath = options.getString("folderPath");

        boolean isSuccess = FileUtil.fileCreateFolder(folderPath);

        JSONObject result = new JSONObject();
        result.put("success", isSuccess);
        result.put("data", isSuccess);
        if(callback != null) {
            callback.invoke(result);
        }
    }




    /** ------------ 压缩包操作 -----------*/
    // 解压ZIP文件（支持加密）
    @UniJSMethod(uiThread = false)
    public void unzipFile(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (unzipFile)   " + options);
        String filePath = options.getString("filePath");
        String password = options.getString("password");

        String zipFinishFilePath = ZipUtil.unzipFile(filePath, password);

        JSONObject result = new JSONObject();
        result.put("success", zipFinishFilePath.length() > 0);
        result.put("data", zipFinishFilePath);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 仅读取ZIP注释信息（不解压/支持加密）
    @UniJSMethod(uiThread = false)
    public void zipGetComments(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (zipGetComments)   " + options);
        String filePath = options.getString("filePath");
        String password = options.getString("password");

        JSONObject zipFileComments = ZipUtil.readZipCommentsOnly(filePath, password);

        JSONObject result = new JSONObject();
        result.put("success", zipFileComments.isEmpty());
        result.put("data", zipFileComments);
        if(callback != null) {
            callback.invoke(result);
        }
    }



    /** ------------ 数据库操作 -----------*/
    // 数据查询
    @UniJSMethod(uiThread = false)
    public void dbQuery(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (dbQuery)   " + options);
        String dbPath = options.getString("dbPath");
        String sql = options.getString("sql");

        List<Map<String, Object>> data = DBUtil.queryToList(dbPath, sql, null);

        JSONObject result = new JSONObject();
        result.put("success", data.isEmpty() ? false : true);
        result.put("data", data);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 查表是否存在
    @UniJSMethod(uiThread = false)
    public void dbTableExist(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (dbTableExist)   " + options);
        String dbPath = options.getString("dbPath");
        String tableName = options.getString("tableName");

        boolean isExist = DBUtil.isTableExists(dbPath, tableName);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("data", isExist);
        if(callback != null) {
            callback.invoke(result);
        }
    }
}
