package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveAction;

public class FileTraverseTask extends RecursiveAction implements Callable<Void> {
    private final List<FileProcessor> processors;
    private final FileCache fileCache;

    public FileTraverseTask(FileCache f, List<FileProcessor> processors) {
        this.processors = processors;
        this.fileCache = f;
    }

    @Override
    public Void call() throws Exception {
        fileCache.forEach(file -> new TraverseTask(fileCache, file, processors));
        return null;
    }

    @Override
    protected void compute() {
        List<TraverseTask> tasks = fileCache.stream().map(file -> new TraverseTask(fileCache, file, processors))
                .toList().blockingGet();
        invokeAll(tasks);
    }
}
