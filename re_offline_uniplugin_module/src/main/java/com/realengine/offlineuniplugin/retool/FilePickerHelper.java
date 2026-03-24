package com.realengine.offlineuniplugin.retool;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FilePickerHelper {
    private static final String TAG = "FilePickerHelper";
    public static final int REQUEST_CODE_PICK_SINGLE_FILE = 10001;
    public static final int REQUEST_CODE_PICK_FOLDER = 10002;

    /**
     * 启动单个文件选择
     * @param activity 当前Activity
     * @param mimeTypes MIME类型数组，
     */
    public static void startPickSingleFile(Activity activity, String[] mimeTypes) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeTypes != null && mimeTypes.length > 0 ? mimeTypes[0] : "*/*");
        if (mimeTypes != null && mimeTypes.length > 1) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CODE_PICK_SINGLE_FILE);
    }

    /**
     * 启动文件夹选择 (Android 5.0+)
     */
    public static void startPickFolder(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    /**
     * 从 onActivityResult 的结果中解析文件信息
     * @param context Context
     * @param data Intent data
     * @return PickerFile 对象，失败返回 null
     */
    public static PickerFile getFileFromResult(Context context, Intent data) {
        if (data == null || data.getData() == null) return null;
        Uri uri = data.getData();
        return getFileInfoFromUri(context, uri);
    }

    /**
     * 从 URI 获取文件信息
     */
    public static PickerFile getFileInfoFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        try {
            ContentResolver resolver = context.getContentResolver();
            String mimeType = resolver.getType(uri);
            String fileName = null;
            long fileSize = 0;

            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                    if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
                }
            }

            if (fileName == null) {
                String path = uri.getPath();
                if (path != null) {
                    fileName = path.substring(path.lastIndexOf('/') + 1);
                } else {
                    fileName = "unknown";
                }
            }

            Log.d(TAG, String.format("文件信息: name=%s, size=%d, mime=%s, uri=%s",
                    fileName, fileSize, mimeType, uri.toString()));

            return new PickerFile(fileName, uri.toString(), fileSize, mimeType);

        } catch (Exception e) {
            Log.e(TAG, "解析文件信息失败", e);
            return null;
        }
    }

    /**
     * 将URI指向的文件复制到应用私有目录
     */
    public static File copyToPrivateDir(Context context, Uri uri, String destFileName) {
        try {
            PickerFile fileInfo = getFileInfoFromUri(context, uri);
            if (fileInfo == null) return null;

            String fileName = destFileName != null ? destFileName : fileInfo.getName();
            File destFile = new File(context.getFilesDir(), fileName);

            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                output.flush();

                Log.d(TAG, "文件已复制到: " + destFile.getAbsolutePath());
                return destFile;

            } catch (Exception e) {
                Log.e(TAG, "复制文件失败", e);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "复制文件失败", e);
            return null;
        }
    }

    public static class PickerFile {
        private String name;
        private String uri;
        private long size;
        private String mimeType;

        public PickerFile(String name, String uri, long size, String mimeType) {
            this.name = name;
            this.uri = uri;
            this.size = size;
            this.mimeType = mimeType;
        }

        public String getName() { return name; }
        public String getUri() { return uri; }
        public long getSize() { return size; }
        public String getMimeType() { return mimeType; }
    }

    // 定义回调接口（可选）
    public interface OnFileSelectedListener {
        void onSuccess(PickerFile file);
        void onError(String error);
        void onCancel();
    }
}