package com.ss.android.ugc.bytex.refercheck.log;

import com.google.common.io.ByteStreams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by tanlehua on 2019/4/15.
 */
class GradleCacheResolver {
    private Map<String, ClassFile> map = new HashMap<>();

    void accept(File cache) {
        try {
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(cache)));
            ZipEntry zipEntry;
            try {
                while ((zipEntry = zin.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    byte[] raw = ByteStreams.toByteArray(zin);
                    String name = zipEntry.getName();
                    if (name.endsWith(".class")) {
                        int classNameEnd = name.lastIndexOf(".class");
                        name = name.substring(0, classNameEnd);
                        String className = name.replaceAll("\\.", "/");
                        map.put(className, new ClassFile(className, cache.getAbsolutePath(), raw));
                    }
                }
            } finally {
                zin.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClassFile {
        String name;
        String jar;
        byte[] content;

        ClassFile(String name, String jar, byte[] content) {
            this.name = name;
            this.jar = jar;
            this.content = content;
        }
    }

    ClassFile get(String className) {
        return map.get(className);
    }
}
