package com.ss.android.ugc.bytex.transformer.processor;

import java.io.IOException;
import java.util.List;

/**
 * Created by tlh on 2018/8/21.
 */

public class ProcessorChain implements FileProcessor.Chain {
    private List<FileProcessor> processors;
    private Input input;
    private int index;

    public ProcessorChain(List<FileProcessor> processors, Input input, int index) {
        this.processors = processors;
        this.input = input;
        this.index = index;
    }

    @Override
    public Input input() {
        return input;
    }

    @Override
    public Output proceed(Input input) throws IOException {
        if (index >= processors.size()) throw new AssertionError();
        FileProcessor next = processors.get(index);
        return next.process(new ProcessorChain(processors, input, index + 1));
    }
}
