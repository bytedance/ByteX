package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class FileTransformTask extends RecursiveAction implements Callable<Void> {
    private final List<FileProcessor> processors;
    private final FileCache fileCache;
    private final TransformContext context;

    public FileTransformTask(TransformContext context, FileCache f, List<FileProcessor> processors) {
        this.context = context;
        this.processors = processors;
        this.fileCache = f;
    }

    @Override
    public Void call() throws Exception {
        fileCache.forEach(file -> new TraverseTask(fileCache, file, processors));
        fileCache.transformOutput();
        return null;
    }

    @Override
    protected void compute() {
        try {
            List<TraverseTask> tasks = fileCache.stream().map(file -> new TraverseTask(fileCache, file, processors))
                    .toList().blockingGet();
            invokeAll(tasks);
            ForkJoinPool.managedBlock(new FileCacheWriter(fileCache));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
