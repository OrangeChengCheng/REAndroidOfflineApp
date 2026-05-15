package com.realengine.offlineuniplugin.uni;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.engine.REEngineActivity;
import com.realengine.offlineuniplugin.reclass.FileInfo;
import com.realengine.offlineuniplugin.retool.FileManager;
import com.realengine.offlineuniplugin.retool.FileUtil;
import com.realengine.offlineuniplugin.retool.FileManagerInterface;
import com.realengine.offlineuniplugin.retool.SafUtil;
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


    /** ------------ 文件操作（SAF） -----------*/
    // 回调接收
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 转发结果给SafUtil，自动触发回调
        SafUtil.handleActivityResult(requestCode, resultCode, data, mUniSDKInstance.getContext());
    }

    // 授权指定的文件夹
    @UniJSMethod(uiThread = false)
    public void safAuthorDocument(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (safAuthorDocument)   " + options);

        final Activity activity = (Activity) mUniSDKInstance.getContext();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SafUtil.openRootFolderPicker(activity, new SafUtil.SimpleFolderPickCallback() {
                    @Override
                    public void onSuccess(String rootFolderPath) {
                        JSONObject result = new JSONObject();
                        result.put("success", rootFolderPath.length() > 0 ? true : false);
                        result.put("data", rootFolderPath);
                        if(callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
            }
        });

    }


    /** ------------ 文件操作（沙盒） -----------*/
    // 获取指定文件夹下所有文件列表
    @UniJSMethod(uiThread = false)
    public void fileGetAllChild(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileGetAllChild)   " + options);
        String filePath = options.getString("filePath");

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        List<FileInfo> fileList = operator.getAllChild(filePath);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        boolean isSuccess = operator.deleteAllFiles(filePath, keepDir);
//        boolean isSuccess = FileUtil.deleteAllFiles(filePath, keepDir);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        boolean isExist = operator.fileExist(filePath);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        List<FileInfo> fileList = operator.getChildFilesBySuffix(filePath, suffix);
//        List<FileInfo> fileList = FileUtil.getChildFilesBySuffix(filePath, suffix);

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
        Log.d(TAG, "************* 【uni -> app】 : (fileGetAppRootFolder)   " + options);

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext());
        FileInfo folder = operator.getAppRootFolder(mUniSDKInstance.getContext());

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), sourceFilePath);
        boolean isSuccess = operator.copyFile(sourceFilePath, targetFilePath);

        JSONObject result = new JSONObject();
        result.put("success", isSuccess);
        result.put("data", isSuccess);
        if(callback != null) {
            callback.invoke(result);
        }
    }

    // 拷贝文件夹（递归）
    @UniJSMethod(uiThread = false)
    public void fileCopyFolder(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileCopyFolder)   " + options);
        String sourceFolderPath = options.getString("sourceFolderPath");
        String targetFolderPath = options.getString("targetFolderPath");

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), sourceFolderPath);
        operator.copyFolder(sourceFolderPath, targetFolderPath, new SafUtil.SimpleCopyProgressCallback() {
            @Override
            public void onProgress(int processed, int totalFiles, String currentFileName) {
                JSONObject result = new JSONObject()
                        .fluentPut("success", true)
                        .fluentPut("data", new JSONObject()
                                .fluentPut("status", "progress")
                                .fluentPut("data", new JSONObject()
                                        .fluentPut("processed", processed)
                                        .fluentPut("totalFiles", totalFiles)
                                        .fluentPut("currentFileName", currentFileName)
                                )
                        );
                if(callback != null) { callback.invokeAndKeepAlive(result); }
            }

            @Override
            public void onSuccess(String destPath) {
                JSONObject result = new JSONObject()
                        .fluentPut("success", true)
                        .fluentPut("data", new JSONObject()
                                .fluentPut("status", "success")
                                .fluentPut("data", destPath)
                        );
                if(callback != null) { callback.invokeAndKeepAlive(result); }
            }
        });
    }

    // 创建文件夹（外部存储）
    @UniJSMethod(uiThread = false)
    public void fileCreateFolder(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (fileCreateFolder)   " + options);
        String folderPath = options.getString("folderPath");

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), folderPath);
        boolean isSuccess = operator.fileCreateFolder(folderPath);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        operator.unzipFileAsync(filePath, password, new ZipUtil.SimpleUnzipProgressCallback() {
            @Override
            public void onProgress(int processed, int totalEntries, String entryName) {
                JSONObject result = new JSONObject()
                        .fluentPut("success", true)
                        .fluentPut("data", new JSONObject()
                                .fluentPut("status", "progress")
                                .fluentPut("data", new JSONObject()
                                        .fluentPut("processed", processed)
                                        .fluentPut("totalEntries", totalEntries)
                                        .fluentPut("entryName", entryName)
                                )
                        );
                if(callback != null) { callback.invokeAndKeepAlive(result); }
            }

            @Override
            public void onSuccess(String destPath) {
                JSONObject result = new JSONObject()
                        .fluentPut("success", true)
                        .fluentPut("data", new JSONObject()
                                .fluentPut("status", "success")
                                .fluentPut("data", destPath)
                        );
                if(callback != null) { callback.invokeAndKeepAlive(result); }
            }
        });
    }

    // 仅读取ZIP注释信息（不解压/支持加密）
    @UniJSMethod(uiThread = false)
    public void zipGetComments(JSONObject options, UniJSCallback callback) {
        Log.d(TAG, "************* 【uni -> app】 : (zipGetComments)   " + options);
        String filePath = options.getString("filePath");

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), filePath);
        JSONObject zipFileComments = operator.readZipCommentsOnly(filePath);
//        JSONObject zipFileComments = ZipUtil.readZipCommentsOnly(filePath, password);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), dbPath);
        List<Map<String, Object>> data = operator.queryToList(dbPath, sql, null);
//        List<Map<String, Object>> data = DBUtil.queryToList(dbPath, sql, null);

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

        FileManagerInterface operator = FileManager.getOperator(mUniSDKInstance.getContext(), dbPath);
        boolean isExist = operator.isTableExists(dbPath, tableName);
//        boolean isExist = DBUtil.isTableExists(dbPath, tableName);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("data", isExist);
        if(callback != null) {
            callback.invoke(result);
        }
    }
}
