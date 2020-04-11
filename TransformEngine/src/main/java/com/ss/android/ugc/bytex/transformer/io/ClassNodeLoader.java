package com.ss.android.ugc.bytex.transformer.io;

import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.cache.JarCache;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Created by tanlehua on 2020-03-09.
 * code is referenced to `lancet`.
 * https://github.com/eleme/lancet
 */
public class ClassNodeLoader implements ClassFinder {
    private final ClassLoader cl;

    public ClassNodeLoader(TransformContext context) {
        URL[] urls = Stream.concat(Stream.concat(context.getAllJars().stream(), context.getAllDirs().stream()),
                getAndroidJar(context).stream()
        )
                .map(FileCache::getFile)
                .map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                })
                .toArray(URL[]::new);
        this.cl = URLClassLoader.newInstance(urls, null);
    }

    @NotNull
    private Collection<JarCache> getAndroidJar(TransformContext context) {
        try {
            return Collections.singleton(new JarCache(context.androidJar(), context));
        } catch (FileNotFoundException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public ClassNode find(String className) {
        try {
            URL url = cl.getResource(className + ".class");
            if (url == null) {
                return null;
            }
            URLConnection urlConnection = url.openConnection();

            // gradle daemon bug:
            // Different builds in one process because of daemon which makes the jar connection will read the context from cache if they points to the same jar file.
            // But the file may be changed.

            urlConnection.setUseCaches(false);
            ClassReader cr = new ClassReader(urlConnection.getInputStream());
            urlConnection.getInputStream().close();
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG);
            return cn;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Class<?> loadClass(String className) {
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
