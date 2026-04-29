package com.realengine.offlineuniplugin.reclass;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import BlackHole3D.REBBox3D;

public class REUnifiedTypeSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (object == null) {
            serializer.out.writeNull();
            return;
        }

        SerializeWriter out = serializer.out;

        // ======================
        // 类型判断与序列化逻辑分发
        // ======================
        if (object instanceof REBBox3D) {
            // 处理REBBox3D类型（复合类型）
            serializeBBox3D(serializer, (REBBox3D) object);
        }
        // 向量类型判断（示例：REDVec3和REIVec2）
        else if (object.getClass().getSimpleName().endsWith("Vec4") ||
                object.getClass().getSimpleName().endsWith("Vec3") ||
                object.getClass().getSimpleName().endsWith("Vec2")) {
            // 处理向量类型（将数值字段序列化为数组）
            serializeVector(serializer, object);
        }
        else {
            // 默认对象序列化（常规Java对象）
            serializeDefaultObject(serializer, object);
        }
    }

    /**
     * 序列化REBBox3D类型（复合类型）
     * 格式：[[min.x, min.y, min.z], [max.x, max.y, max.z]]
     */
    private void serializeBBox3D(JSONSerializer serializer, REBBox3D bbox) throws IOException {
        SerializeWriter out = serializer.out;
        out.write('['); // 开始外层数组

        // 序列化min向量
        serializer.write(bbox.getMin());
        out.write(','); // 分隔符

        // 序列化max向量
        serializer.write(bbox.getMax());

        out.write(']'); // 结束外层数组
    }

    /**
     * 序列化向量类型（REDVec3、REIVec2等）
     * 格式：[field1, field2, field3]（仅包含数值字段）
     */
    private void serializeVector(JSONSerializer serializer, Object vector) throws IOException {
        SerializeWriter out = serializer.out;
        out.write('['); // 开始数组

        Class<?> clazz = vector.getClass();
        Field[] fields = clazz.getDeclaredFields();

        // 存储所有有效数字字段的值
        List<Number> values = new ArrayList<>();

        // 检查是否为四元数结构 {w,x,y,z}
        boolean isQuaternion = false;
        if (fields.length == 4) {
            String[] fieldNames = new String[4];
            for (int i = 0; i < 4; i++) {
                fieldNames[i] = fields[i].getName();
            }
            isQuaternion = "w".equals(fieldNames[0]) &&
                    "x".equals(fieldNames[1]) &&
                    "y".equals(fieldNames[2]) &&
                    "z".equals(fieldNames[3]);
        }

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(vector);
                if (value instanceof Number) {
                    values.add((Number) value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // 处理四元数顺序转换 [w,x,y,z] -> [x,y,z,w]
        if (isQuaternion && values.size() == 4) {
            serializer.write(values.get(1)); // x
            out.write(',');
            serializer.write(values.get(2)); // y
            out.write(',');
            serializer.write(values.get(3)); // z
            out.write(',');
            serializer.write(values.get(0)); // w
        } else {
            // 普通向量按原顺序输出
            for (int i = 0; i < values.size(); i++) {
                serializer.write(values.get(i));
                if (i < values.size() - 1) {
                    out.write(',');
                }
            }
        }

        out.write(']'); // 结束数组
    }

    /**
     * 序列化常规Java对象（默认处理）
     * 格式：{field1: value1, field2: value2, ...}
     */
    private void serializeDefaultObject(JSONSerializer serializer, Object object) throws IOException {
        SerializeWriter out = serializer.out;
        out.write('{'); // 开始对象

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);

            try {
                Object value = field.get(object);
                if (value != null) {
                    out.writeFieldName(field.getName());
                    serializer.write(value);
                    // 非最后一个元素时添加逗号
                    if (i < fields.length - 1) {
                        out.write(',');
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        out.write('}'); // 结束对象
    }

}
