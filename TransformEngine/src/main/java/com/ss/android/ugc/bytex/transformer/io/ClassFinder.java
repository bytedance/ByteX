package com.ss.android.ugc.bytex.transformer.io;

import org.objectweb.asm.tree.ClassNode;

/**
 * Created by tanlehua on 2020-03-09.
 */
public interface ClassFinder {
    ClassNode find(String className);

    Class<?> loadClass(String className);
}
