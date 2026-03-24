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
 * 纯静态SQLite通用工具类
 * 核心特性：
 * 1. 无业务逻辑，仅做通用DB操作
 * 2. 支持动态指定DB路径+SQL语句
 * 3. 内置异常处理（路径错误、DB打不开、SQL错误等）
 * 4. 静态调用，无需创建实例
 * 5. 内部缓存多DB实例，优化性能
 */
public final class DBUtil {
    // 日志TAG
    private static final String TAG = "DBUtil";
    // 线程安全的DB实例缓存：key=db文件路径，value=对应的SQLiteDatabase实例
    private static final Map<String, SQLiteDatabase> DB_CACHE = new ConcurrentHashMap<>();

    // 私有化构造函数，禁止外部实例化（纯静态工具类）
    private DBUtil() {
        throw new UnsupportedOperationException("DBUtil is a static tool class, cannot be instantiated!");
    }

    // ------------------- 核心内部方法：获取/缓存DB实例 -------------------
    /**
     * 内部方法：根据DB路径获取SQLiteDatabase实例（缓存优化）
     * @param dbPath 完整的.db文件路径（如：/data/data/包名/files/mydb.db）
     * @return 可用的SQLiteDatabase实例，失败返回null
     */
    private static SQLiteDatabase getDatabase(String dbPath) {
        // 空值/空路径校验
        if (dbPath == null || dbPath.trim().isEmpty()) {
            Log.e(TAG, "getDatabase failed: dbPath is null or empty");
            return null;
        }

        // 先从缓存获取
        SQLiteDatabase db = DB_CACHE.get(dbPath);
        if (db != null && db.isOpen()) {
            return db;
        }

        // 缓存无实例，尝试打开DB文件
        try {
            File dbFile = new File(dbPath);
            // 检查文件是否存在
            if (!dbFile.exists()) {
                Log.e(TAG, "getDatabase failed: DB file not exist -> " + dbPath);
                return null;
            }
            // 检查文件是否是合法文件（不是目录）
            if (dbFile.isDirectory()) {
                Log.e(TAG, "getDatabase failed: dbPath is a directory -> " + dbPath);
                return null;
            }

            // 打开数据库（读写模式，若只读可改为 OPEN_READONLY）
            db = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );
            // 存入缓存
            DB_CACHE.put(dbPath, db);
            Log.d(TAG, "Database opened successfully -> " + dbPath);
            return db;
        } catch (SQLException e) {
            Log.e(TAG, "getDatabase failed: open DB error -> " + dbPath + ", error: " + e.getMessage());
            return null;
        } catch (SecurityException e) {
            Log.e(TAG, "getDatabase failed: no permission to access DB -> " + dbPath + ", error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getDatabase failed: unknown error -> " + dbPath + ", error: " + e.getMessage());
            return null;
        }
    }

    // ------------------- 对外通用方法：查询（返回Cursor） -------------------
    /**
     * 执行查询SQL，返回Cursor（外部需自行关闭Cursor！）
     * @param dbPath DB文件完整路径
     * @param sql 查询SQL语句（如：SELECT * FROM users WHERE age > ?）
     * @param selectionArgs SQL中?对应的参数数组（如：new String[]{"20"}）
     * @return 成功返回Cursor，失败返回null
     */
    public static Cursor query(String dbPath, String sql, String[] selectionArgs) {
        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) {
            return null;
        }

        try {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            Log.d(TAG, "query success -> sql: " + sql);
            return cursor;
        } catch (SQLException e) {
            Log.e(TAG, "query failed -> sql: " + sql + ", error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "query failed: unknown error -> sql: " + sql + ", error: " + e.getMessage());
            return null;
        }
    }

    // ------------------- 对外通用方法：查询（返回结构化List，无需手动关Cursor） -------------------
    /**
     * 执行查询SQL，返回结构化List（内部自动关闭Cursor，更安全）
     * @param dbPath DB文件完整路径
     * @param sql 查询SQL语句
     * @param selectionArgs SQL参数数组
     * @return List<Map<String, Object>>：每个Map对应一行数据，key=列名，value=列值
     */
    public static List<Map<String, Object>> queryToList(String dbPath, String sql, String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = query(dbPath, sql, selectionArgs);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.d(TAG, "queryToList: no data -> sql: " + sql);
                return Collections.emptyList();
            }

            // 解析Cursor为List<Map>
            List<Map<String, Object>> resultList = new ArrayList<>();
            int columnCount = cursor.getColumnCount();
            String[] columnNames = cursor.getColumnNames();

            do {
                Map<String, Object> rowMap = new HashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    String columnName = columnNames[i];
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            rowMap.put(columnName, null);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            rowMap.put(columnName, cursor.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            rowMap.put(columnName, cursor.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            rowMap.put(columnName, cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            rowMap.put(columnName, cursor.getBlob(i));
                            break;
                        default:
                            rowMap.put(columnName, cursor.getString(i));
                            break;
                    }
                }
                resultList.add(rowMap);
            } while (cursor.moveToNext());

            Log.d(TAG, "queryToList success -> sql: " + sql + ", count: " + resultList.size());
            return resultList;
        } catch (Exception e) {
            Log.e(TAG, "queryToList failed -> sql: " + sql + ", error: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            // 内部自动关闭Cursor，避免资源泄漏
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    // ------------------- 新增：检查表是否存在（核心方法，返回布尔值） -------------------
    /**
     * 检查指定数据库中是否存在指定名称的表
     * @param dbPath DB文件完整路径
     * @param tableName 要检查的表名（支持特殊字符/长表名）
     * @return true=表存在，false=表不存在/DB打开失败/参数错误
     */
    public static boolean isTableExists(String dbPath, String tableName) {
        // 前置参数校验
        if (dbPath == null || dbPath.trim().isEmpty() || tableName == null || tableName.trim().isEmpty()) {
            Log.e(TAG, "isTableExists failed: dbPath or tableName is invalid");
            return false;
        }

        // 构建检查表存在的SQL（参数化查询，避免SQL注入/特殊字符问题）
        String checkSql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?;";
        Cursor cursor = null;
        try {
            cursor = query(dbPath, checkSql, new String[]{tableName.trim()});
            // 解析结果：count(*)为1则存在，0则不存在
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                boolean exists = count == 1;
                Log.d(TAG, "isTableExists -> table: " + tableName + ", exists: " + exists);
                return exists;
            }
        } catch (Exception e) {
            Log.e(TAG, "isTableExists failed -> table: " + tableName + ", error: " + e.getMessage());
        } finally {
            // 关闭Cursor，避免资源泄漏
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return false;
    }

    // ------------------- 新增：兼容queryToList格式的表存在查询（可选） -------------------
    /**
     * 检查表是否存在，返回queryToList格式的结果（便于统一处理返回值）
     * @param dbPath DB文件完整路径
     * @param tableName 要检查的表名
     * @return List<Map<String, Object>>：包含一个key="exists"，value=Boolean的Map
     */
    public static List<Map<String, Object>> checkTableExistsToList(String dbPath, String tableName) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("exists", isTableExists(dbPath, tableName));
        List<Map<String, Object>> resultList = new ArrayList<>();
        resultList.add(resultMap);
        return resultList;
    }

    // ------------------- 对外通用方法：执行增/删/改（SQL语句方式） -------------------
    /**
     * 执行增/删/改SQL（如：INSERT INTO users(name) VALUES('张三')）
     * @param dbPath DB文件完整路径
     * @param sql 增/删/改SQL语句
     * @param bindArgs SQL中?对应的参数数组
     * @return 成功返回受影响行数，失败返回-1
     */
    public static int executeUpdate(String dbPath, String sql, Object[] bindArgs) {
        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) {
            return -1;
        }

        try {
            // 开始事务，保证操作原子性
            db.beginTransaction();
            if (bindArgs == null || bindArgs.length == 0) {
                db.execSQL(sql);
            } else {
                db.execSQL(sql, bindArgs);
            }
            // 标记事务成功
            db.setTransactionSuccessful();
            // 执行后获取变更行数（注：execSQL无返回值，若需精确行数，建议用下面的insert/update/delete方法）
            Log.d(TAG, "executeUpdate success -> sql: " + sql);
            return 1; // 通用成功标识（若需精确行数，改用ContentValues方式）
        } catch (SQLException e) {
            Log.e(TAG, "executeUpdate failed -> sql: " + sql + ", error: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "executeUpdate failed: unknown error -> sql: " + sql + ", error: " + e.getMessage());
            return -1;
        } finally {
            // 结束事务
            if (db.isOpen()) {
                db.endTransaction();
            }
        }
    }

    // ------------------- 对外通用方法：插入（ContentValues方式，返回行ID） -------------------
    /**
     * 插入数据（ContentValues方式，更安全，避免SQL注入）
     * @param dbPath DB文件完整路径
     * @param tableName 表名
     * @param values 插入的键值对
     * @return 成功返回插入行的ID，失败返回-1
     */
    public static long insert(String dbPath, String tableName, ContentValues values) {
        if (tableName == null || tableName.isEmpty() || values == null || values.size() == 0) {
            Log.e(TAG, "insert failed: tableName or values is invalid");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) {
            return -1;
        }

        try {
            long rowId = db.insert(tableName, null, values);
            Log.d(TAG, "insert success -> table: " + tableName + ", rowId: " + rowId);
            return rowId;
        } catch (SQLException e) {
            Log.e(TAG, "insert failed -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "insert failed: unknown error -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        }
    }

    // ------------------- 对外通用方法：更新（ContentValues方式，返回受影响行数） -------------------
    /**
     * 更新数据（ContentValues方式）
     * @param dbPath DB文件完整路径
     * @param tableName 表名
     * @param values 更新的键值对
     * @param whereClause WHERE条件（如：id = ?）
     * @param whereArgs WHERE条件参数（如：new String[]{"1"}）
     * @return 成功返回受影响行数，失败返回-1
     */
    public static int update(String dbPath, String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        if (tableName == null || tableName.isEmpty() || values == null || values.size() == 0) {
            Log.e(TAG, "update failed: tableName or values is invalid");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) {
            return -1;
        }

        try {
            int affectedRows = db.update(tableName, values, whereClause, whereArgs);
            Log.d(TAG, "update success -> table: " + tableName + ", affectedRows: " + affectedRows);
            return affectedRows;
        } catch (SQLException e) {
            Log.e(TAG, "update failed -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "update failed: unknown error -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        }
    }

    // ------------------- 对外通用方法：删除（返回受影响行数） -------------------
    /**
     * 删除数据
     * @param dbPath DB文件完整路径
     * @param tableName 表名
     * @param whereClause WHERE条件（如：id = ?）
     * @param whereArgs WHERE条件参数
     * @return 成功返回受影响行数，失败返回-1
     */
    public static int delete(String dbPath, String tableName, String whereClause, String[] whereArgs) {
        if (tableName == null || tableName.isEmpty()) {
            Log.e(TAG, "delete failed: tableName is invalid");
            return -1;
        }

        SQLiteDatabase db = getDatabase(dbPath);
        if (db == null) {
            return -1;
        }

        try {
            int affectedRows = db.delete(tableName, whereClause, whereArgs);
            Log.d(TAG, "delete success -> table: " + tableName + ", affectedRows: " + affectedRows);
            return affectedRows;
        } catch (SQLException e) {
            Log.e(TAG, "delete failed -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "delete failed: unknown error -> table: " + tableName + ", error: " + e.getMessage());
            return -1;
        }
    }

    // ------------------- 对外通用方法：关闭指定DB -------------------
    /**
     * 关闭指定路径的DB实例，清理缓存
     * @param dbPath DB文件完整路径
     */
    public static void closeDatabase(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return;
        }

        SQLiteDatabase db = DB_CACHE.remove(dbPath);
        if (db != null && db.isOpen()) {
            try {
                db.close();
                Log.d(TAG, "closeDatabase success -> " + dbPath);
            } catch (Exception e) {
                Log.e(TAG, "closeDatabase failed -> " + dbPath + ", error: " + e.getMessage());
            }
        }
    }

    // ------------------- 对外通用方法：关闭所有DB -------------------
    /**
     * 关闭所有已打开的DB实例，清空缓存（建议在App退出时调用）
     */
    public static void closeAllDatabases() {
        if (DB_CACHE.isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<String, SQLiteDatabase> entry : DB_CACHE.entrySet()) {
                SQLiteDatabase db = entry.getValue();
                if (db != null && db.isOpen()) {
                    db.close();
                    Log.d(TAG, "closeAllDatabases: closed -> " + entry.getKey());
                }
            }
            DB_CACHE.clear();
            Log.d(TAG, "closeAllDatabases: all DB closed");
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "closeAllDatabases failed: concurrent modification -> " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "closeAllDatabases failed: unknown error -> " + e.getMessage());
        }
    }
}