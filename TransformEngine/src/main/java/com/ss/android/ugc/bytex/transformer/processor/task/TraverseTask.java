package com.ss.android.ugc.bytex.transformer.processor.task;

import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.cache.FileData;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.ProcessorChain;
import com.ss.android.ugc.bytex.transformer.processor.Input;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveAction;

public class TraverseTask extends RecursiveAction implements Callable<Void> {
    private final FileCache fileCache;
    private final FileData file;
    private final List<FileProcessor> processors;


    public TraverseTask(FileCache fileCache, FileData file, List<FileProcessor> processors) {
        this.fileCache = fileCache;
        this.file = file;
        this.processors = processors;
    }

    @Override
    protected void compute() {
        try {
            Input input = new Input(fileCache.getContent(), file);
            ProcessorChain chain = new ProcessorChain(processors, input, 0);
            chain.proceed(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void call() throws Exception {
        compute();
        return null;
    }
}