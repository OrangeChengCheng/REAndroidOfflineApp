package com.realengine.offlineuniplugin.retool;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLite 数据库通用工具类
 * <p>
 * 功能说明：
 * 1. 支持动态指定 .db 数据库文件路径，无需继承 SQLiteOpenHelper
 * 2. 提供查询、新增、修改、删除、检查表是否存在等完整操作
 * 3. 内部使用 ConcurrentHashMap 缓存数据库连接，提升性能
 * 4. 全方法异常捕获，自动日志打印，上层调用更安全
 * 5. 自动管理 Cursor 关闭，避免内存泄漏
 * 6. 支持事务、支持参数化 SQL，防止注入
 * <p>
 * 使用方式：全部静态调用，不可实例化
 *
 * @author RealEngine
 * @date 2025
 */
public final class DBUtil {
    /**
     * 日志标签
     */
    private static final String TAG = "DBUtil";

    /**
     * 数据库连接缓存（线程安全）
     * key = 数据库绝对路径
     * value = 已打开的 SQLiteDatabase 实例
     */
    private static final Map<String, SQLiteDatabase> DB_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有化构造方法
     * 禁止外部实例化，强制使用静态方法
     */
    private DBUtil() {
        throw new UnsupportedOperationException("DBUtil is a static tool class, cannot be instantiated!");
    }

    // ============================== 内部核心方法 ==============================

    /**
     * 获取数据库连接（带缓存，自动复用）
     * 校验文件是否存在 → 打开数据库 → 存入缓存
     *
     * @param dbPath 数据库 .db 文件绝对路径
     * @return 可用的数据库实例，失败返回 null
     */
    private static SQLiteDatabase getDatabase(String dbPath) {
        // 路径为空校验
        if (dbPath == null || dbPath.trim().isEmpty()) {
            Log.e(TAG, "getDatabase 失败：数据库路径为空");
            return null;
        }

        // 从缓存获取已打开的连接
        SQLiteDatabase db = DB_CACHE.get(dbPath);
        if (db != null && db.isOpen()) {
            return db;
        }

        // 缓存无连接，重新打开
        try {
            File dbFile = new File(dbPath);

            // 文件不存在
            if (!dbFile.exists()) {
                Log.e(TAG, "getDatabase 失败：数据库文件不存在 → " + dbPath);
                return null;
            }

            // 路径是文件夹而非文件
            if (dbFile.isDirectory()) {
                Log.e(TAG, "getDatabase 失败：路径是目录而非数据库文件 → " + dbPath);
                return null;
            }

            // 打开读写模式数据库
            db = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );

            // 加入缓存
            DB_CACHE.put(dbPath, db);
            Log.d(TAG, "数据库打开成功 → " + dbPath);
            return db;

        } catch (SQLException e) {
            Log.e(TAG, "getDatabase 数据库打开异常 → " + dbPath, e);
        } catch (SecurityException e) {
            Log.e(TAG, "getDatabase 无访问权限 → " + dbPath, e);
        } catch (Exception e) {
            Log.e(TAG, "getDatabase 未知异常 → " + dbPath, e);
        }

        return null;
    }

    // ============================== 对外查询方法 ==============================

    /**
     * 执行查询 SQL，返回原始 Cursor
     * 注意：外部使用完必须手动关闭 cursor
     *
     * @param dbPath        数据库路径
     * @param sql           查询语句，支持 ? 占位符
     * @param selectionArgs 占位符参数
     * @return Cursor 对象，失败返回 null
     */
    public static Cursor query(String dbPath, String sql, String[] selectionArgs) {
        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) return null;

        try {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            Log.d(TAG, "query 执行成功 → " + sql);
            return cursor;
        } catch (Exception e) {
            Log.e(TAG, "query 执行失败 → " + sql, e);
        }
        return null;
    }

    /**
     * 执行查询，返回 List<Map<String, Object>> 结构
     * 内部自动关闭 cursor，最推荐使用
     *
     * @param dbPath        数据库路径
     * @param sql           查询语句
     * @param selectionArgs 参数
     * @return 数据列表，无数据返回空列表
     */
    public static List<Map<String, Object>> queryToList(String dbPath, String sql, String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = query(dbPath, sql, selectionArgs);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.d(TAG, "queryToList 无数据 → " + sql);
                return Collections.emptyList();
            }

            List<Map<String, Object>> resultList = new ArrayList<>();
            int columnCount = cursor.getColumnCount();
            String[] columnNames = cursor.getColumnNames();

            // 遍历每一行数据
            do {
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    // 根据字段类型自动取值
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            row.put(columnNames[i], null);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(columnNames[i], cursor.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            row.put(columnNames[i], cursor.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(columnNames[i], cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            row.put(columnNames[i], cursor.getBlob(i));
                            break;
                        default:
                            row.put(columnNames[i], cursor.getString(i));
                    }
                }
                resultList.add(row);
            } while (cursor.moveToNext());

            Log.d(TAG, "queryToList 成功 → 数据条数：" + resultList.size());
            return resultList;

        } catch (Exception e) {
            Log.e(TAG, "queryToList 异常 → " + sql, e);
        } finally {
            // 确保关闭游标，避免泄漏
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return Collections.emptyList();
    }

    /**
     * 检查数据库中是否存在指定表
     *
     * @param dbPath    数据库路径
     * @param tableName 表名
     * @return true 存在，false 不存在/异常
     */
    public static boolean isTableExists(String dbPath, String tableName) {
        if (dbPath == null || tableName == null || dbPath.isEmpty() || tableName.isEmpty()) {
            Log.e(TAG, "isTableExists 参数无效");
            return false;
        }

        String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?";
        Cursor cursor = null;

        try {
            cursor = query(dbPath, sql, new String[]{tableName.trim()});
            if (cursor != null && cursor.moveToFirst()) {
                boolean exists = cursor.getInt(0) == 1;
                Log.d(TAG, "isTableExists → 表 " + tableName + " 存在：" + exists);
                return exists;
            }
        } catch (Exception e) {
            Log.e(TAG, "isTableExists 异常 → " + tableName, e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * 检查表是否存在，返回统一的 List Map 格式
     *
     * @param dbPath    数据库路径
     * @param tableName 表名
     * @return 包含 exists 字段的集合
     */
    public static List<Map<String, Object>> checkTableExistsToList(String dbPath, String tableName) {
        Map<String, Object> result = new HashMap<>();
        result.put("exists", isTableExists(dbPath, tableName));

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(result);
        return list;
    }

    // ============================== 增删改操作 ==============================

    /**
     * 执行更新类 SQL（insert / update / delete / create 等）
     * 自带事务保证原子性
     *
     * @param dbPath   数据库路径
     * @param sql      执行语句
     * @param bindArgs 参数
     * @return 成功返回 1，失败返回 -1
     */
    public static int executeUpdate(String dbPath, String sql, Object[] bindArgs) {
        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) return -1;

        try {
            db.beginTransaction();

            if (bindArgs == null || bindArgs.length == 0) {
                db.execSQL(sql);
            } else {
                db.execSQL(sql, bindArgs);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "executeUpdate 执行成功 → " + sql);
            return 1;

        } catch (Exception e) {
            Log.e(TAG, "executeUpdate 执行失败 → " + sql, e);
        } finally {
            if (db.isOpen()) {
                db.endTransaction();
            }
        }

        return -1;
    }

    /**
     * 插入数据（安全推荐，防 SQL 注入）
     *
     * @param dbPath    数据库路径
     * @param tableName 表名
     * @param values    键值对数据
     * @return 成功返回新行ID，失败返回 -1
     */
    public static long insert(String dbPath, String tableName, ContentValues values) {
        if (tableName == null || tableName.isEmpty() || values == null || values.size() == 0) {
            Log.e(TAG, "insert 参数无效：表名或数据为空");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) return -1;

        try {
            long rowId = db.insert(tableName, null, values);
            Log.d(TAG, "insert 成功 → 表：" + tableName + " 行ID：" + rowId);
            return rowId;
        } catch (Exception e) {
            Log.e(TAG, "insert 异常 → " + tableName, e);
        }
        return -1;
    }

    /**
     * 更新数据
     *
     * @param dbPath      数据库路径
     * @param tableName   表名
     * @param values      要更新的数据
     * @param whereClause 条件语句（如 id=?）
     * @param whereArgs   条件参数
     * @return 受影响行数
     */
    public static int update(String dbPath, String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        if (tableName == null || tableName.isEmpty() || values == null || values.size() == 0) {
            Log.e(TAG, "update 参数无效");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) return -1;

        try {
            int rows = db.update(tableName, values, whereClause, whereArgs);
            Log.d(TAG, "update 成功 → 表：" + tableName + " 影响行数：" + rows);
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "update 异常 → " + tableName, e);
        }
        return -1;
    }

    /**
     * 删除数据
     *
     * @param dbPath      数据库路径
     * @param tableName   表名
     * @param whereClause 条件
     * @param whereArgs   参数
     * @return 受影响行数
     */
    public static int delete(String dbPath, String tableName, String whereClause, String[] whereArgs) {
        if (tableName == null || tableName.isEmpty()) {
            Log.e(TAG, "delete 参数无效：表名不能为空");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) return -1;

        try {
            int rows = db.delete(tableName, whereClause, whereArgs);
            Log.d(TAG, "delete 成功 → 表：" + tableName + " 影响行数：" + rows);
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "delete 异常 → " + tableName, e);
        }
        return -1;
    }

    // ============================== 数据库关闭 ==============================

    /**
     * 关闭指定数据库连接，并清理缓存
     *
     * @param dbPath 数据库路径
     */
    public static void closeDatabase(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) return;

        SQLiteDatabase db = DB_CACHE.remove(dbPath);
        if (db != null && db.isOpen()) {
            try {
                db.close();
                Log.d(TAG, "closeDatabase 成功 → " + dbPath);
            } catch (Exception e) {
                Log.e(TAG, "closeDatabase 异常 → " + dbPath, e);
            }
        }
    }

    /**
     * 关闭所有数据库连接
     * 建议在 APP 退出时调用
     */
    public static void closeAllDatabases() {
        if (DB_CACHE.isEmpty()) return;

        try {
            for (Map.Entry<String, SQLiteDatabase> entry : DB_CACHE.entrySet()) {
                SQLiteDatabase db = entry.getValue();
                if (db != null && db.isOpen()) {
                    db.close();
                    Log.d(TAG, "关闭数据库 → " + entry.getKey());
                }
            }
            DB_CACHE.clear();
            Log.d(TAG, "所有数据库已关闭");
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "closeAllDatabases 并发修改异常", e);
        } catch (Exception e) {
            Log.e(TAG, "closeAllDatabases 未知异常", e);
        }
    }
}