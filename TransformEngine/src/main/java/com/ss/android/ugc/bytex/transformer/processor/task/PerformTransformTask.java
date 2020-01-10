package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformTransformTask extends RecursiveAction {
    private final List<FileProcessor> processors;
    private final boolean outputFile;
    private final Stream<FileCache> source;
    private final TransformContext context;

    public PerformTransformTask(Stream<FileCache> source, List<FileProcessor> processors, boolean outputFile, TransformContext context) {
        this.processors = processors;
        this.source = source;
        this.outputFile = outputFile;
        this.context = context;
    }

    @Override
    protected void compute() {
        if (outputFile) {
            List<FileTransformTask> tasks = source.map(cache -> new FileTransformTask(context, cache, processors)).collect(Collectors.toList());
            invokeAll(tasks);
        } else {
            PerformTraverseTask traverseTask = new PerformTraverseTask(source, processors);
            invokeAll(traverseTask);
        }
    }
}
