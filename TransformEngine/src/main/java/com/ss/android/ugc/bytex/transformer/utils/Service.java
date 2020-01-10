package com.ss.android.ugc.bytex.transformer.utils;

import java.util.ServiceLoader;

/**
 * Created by tanlehua on 2019-04-29.
 */
public class Service {
    public static <T> T load(Class<T> clazz) {
        try {
            return ServiceLoader.load(clazz, Service.class.getClassLoader()).iterator().next();
        } catch (Exception e) {
            return null;
        }
    }
}
