package com.ss.android.ugc.bytex.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

/**
 * Created by tanlehua on 2019-07-14.
 */
public class ReflectionUtils {
    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int originalModifier = field.getModifiers();

        try {
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, newValue);
        } finally {
            modifiersField.setInt(field, originalModifier);
        }

    }

    public static int makeFinalFieldAccessible(Field field) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int originalModifier = field.getModifiers();
        try {
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception e) {
            modifiersField.setInt(field, originalModifier);
        }
        field.setAccessible(true);
        return originalModifier;
    }

    public static void setModifier(Field field, int mod) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int originalModifier = field.getModifiers();
        try {
            modifiersField.setInt(field, mod);
        } catch (Exception e) {
            modifiersField.setInt(field, originalModifier);
        }
    }

    public static Unsafe getUnsafe() throws Exception {
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        return (Unsafe) unsafe.get(null);
    }

    public static long getObjectAddress(Object obj) {
        try {
            Unsafe unsafe = getUnsafe();
            Object[] array = new Object[]{obj};
            if (unsafe.arrayIndexScale(Object[].class) == 8) {
                return unsafe.getLong(array, unsafe.arrayBaseOffset(Object[].class));
            } else {
                return 0xffffffffL & unsafe.getInt(array, unsafe.arrayBaseOffset(Object[].class));
            }
        } catch (Exception e) {
            return -1;
        }
    }


    public static <T> T getField(Class clazz, Object target, String fieldName) throws Exception {
        Field declaredField = clazz.getDeclaredField(fieldName);
        declaredField.setAccessible(true);
        return (T) declaredField.get(target);
    }

    public static <T> T getField(Object target, String fieldName) throws Exception {
        return getField(target.getClass(), target, fieldName);
    }

    public static <T> T invokeMethod(Object target, String methodName, Object... args) throws Exception {
        return invokeMethod(target.getClass(), target, methodName, args);
    }

    public static <T> T invokeMethod(Class clazz, Object target, String methodName, Object... args) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    public static void setStaticField(Class clazz, String field, Object newValue) throws Exception {
        Field declaredField = clazz.getDeclaredField(field);
        setStaticField(declaredField, newValue);
    }

    public static void setStaticField(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    public static void setFiled(Object target, String filedName, Object fieldValue) throws Exception {
        setFiled(target.getClass(), target, filedName, fieldValue);
    }

    public static void setFiled(Class<?> clazz, Object target, String filedName, Object fieldValue) throws Exception {
        Field declaredField = clazz.getDeclaredField(filedName);
        declaredField.setAccessible(true);
        declaredField.set(target, fieldValue);
    }
}
