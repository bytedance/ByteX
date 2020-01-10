package com.ss.android.ugc.bytex.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * This is not a robust data structure.
 *
 * @param <T>
 */
public class Bucket<T> {
    private T[] data;
    private int size;
    private IndexFunction indexFunction;

    public Bucket(T[] data, IndexFunction function) {
        this.data = data;
        this.size = data.length;
        this.indexFunction = function;
    }

    public T get(String key) {
        return data[indexFunction.getIndex(key)];
    }

    public void put(String key, T value) {
        data[indexFunction.getIndex(key)] = value;
    }

    public void set(int index, T value) {
        data[index] = value;
    }

    public T computIfAbsent(String key, Function<String, ? extends T> func) {
        int index = indexFunction.getIndex(key);
        T elem = data[index];
        if (elem == null) {
            elem = func.apply(key);
            data[index] = elem;
        }
        return elem;
    }

    public List<T> asList() {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, data);
        return list;
    }

    public int size() {
        return size;
    }

    public void release() {
        data = null;
    }

    public interface IndexFunction {
        int getIndex(String key);
    }
}
