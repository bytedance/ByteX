package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.cache.FileCache;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public class FileCacheWriter implements ForkJoinPool.ManagedBlocker {
    private final FileCache fileCache;
    private volatile boolean finish;

    public FileCacheWriter(FileCache fileCache) {
        this.fileCache = fileCache;
    }

    @Override
    public boolean block() throws InterruptedException {
        if (!finish) {
            try {
                fileCache.transformOutput();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                finish = true;
            }
        }
        return true;
    }

    @Override
    public boolean isReleasable() {
        return finish;
    }
}
