package com.ss.android.ugc.bytex.transformer.io;


import com.android.build.gradle.AppExtension;

import org.gradle.api.Project;
import org.gradle.internal.hash.Hashing;
import org.gradle.wrapper.Download;
import org.gradle.wrapper.ExclusiveFileAccessManager;
import org.gradle.wrapper.Logger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AndroidJarProvider {
    AndroidJarProvider DEFAULT = new AndroidJarProvider() {
        @Override
        public File getAndroidJar(@Nonnull Project project, @Nonnull AppExtension android) {
            return new File(String.join(File.separator,
                    android.getSdkDirectory().getAbsolutePath(),
                    "platforms",
                    android.getCompileSdkVersion(),
                    "android.jar")
            );
        }
    };

    final class URIAndroidJarProvider implements AndroidJarProvider {
        private static final Map<URI, URIAndroidJarProvider> cachingAndroidJarProviders = new ConcurrentHashMap<>();

        public static AndroidJarProvider obtain(URI uri) {
            return cachingAndroidJarProviders.computeIfAbsent(uri, new Function<URI, URIAndroidJarProvider>() {
                @Override
                public URIAndroidJarProvider apply(URI uri) {
                    return new URIAndroidJarProvider(uri);
                }
            });
        }

        private final URI uri;
        private final String name;

        private URIAndroidJarProvider(URI uri) {
            this.uri = uri;
            String path = uri.getPath();
            String lastPathName = path.substring(path.lastIndexOf("/"));
            if (lastPathName.endsWith(".jar")) {
                name = lastPathName;
            } else {
                name = lastPathName + ".jar";
            }
        }

        @Nullable
        @Override
        public synchronized File getAndroidJar(@Nonnull Project project, @Nonnull AppExtension android) {
            try {
                final File androidJarCacheFile = new File(project.getGradle().getGradleUserHomeDir(), "caches/androidJar/" + name + "-" + Hashing.hashString(uri.toString()) + "/" + name);
                if (androidJarCacheFile.exists()) {
                    return androidJarCacheFile;
                }
                return new ExclusiveFileAccessManager(120000, 200).access(androidJarCacheFile, new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        if (androidJarCacheFile.exists()) {
                            return androidJarCacheFile;
                        }
                        File tempFile = new File(androidJarCacheFile.getParentFile(), androidJarCacheFile.getName() + ".part");
                        tempFile.delete();
                        new Download(new Logger(false), "androidJar", project.getGradle().getGradleVersion()).download(safeUri(uri), tempFile);
                        tempFile.renameTo(androidJarCacheFile);
                        return androidJarCacheFile;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                cachingAndroidJarProviders.remove(uri);
            }
        }

        static URI safeUri(URI uri) throws URISyntaxException {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
    }

    @Nullable
    File getAndroidJar(@Nonnull Project project, @Nonnull AppExtension android);
}