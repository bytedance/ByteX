package com.ss.android.ugc.bytex.const_inline.reflect.model;

/**
 * Created by yangzhiqian on 2019/4/21<br/>
 * Desc:
 */
public abstract class ReflectModel {
    public static final int REFLECT_TYPE_FIELD = 1;
    public static final int REFLECT_TYPE_DECLARED_FIELD = 2;
    public static final int REFLECT_TYPE_FIELDS = 3;
    public static final int REFLECT_TYPE_DECLARED_FIELDS = 4;

//    public static final int REFLECT_TYPE_METHOD = 11;
//    public static final int REFLECT_TYPE_DECLARED_METHOD = 12;
//    public static final int REFLECT_TYPE_METHODS = 13;

    public final String owner;

    public ReflectModel() {
        this(null);
    }

    public ReflectModel(String owner) {
        this.owner = owner;
    }

    public abstract int getReflectType();
}