package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformTraverseTask extends RecursiveAction {
    private final Stream<FileCache> source;
    private final List<FileProcessor> processors;

    public PerformTraverseTask(Stream<FileCache> source, List<FileProcessor> processors) {
        this.source = source;
        this.processors = processors;
    }

    @Override
    protected void compute() {
        List<FileTraverseTask> tasks = source.map(cache -> new FileTraverseTask(cache, processors)).collect(Collectors.toList());
        invokeAll(tasks);
    }
}
