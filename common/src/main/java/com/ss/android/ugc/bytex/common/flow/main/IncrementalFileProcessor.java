package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.transformer.processor.ClassFileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.Input;
import com.ss.android.ugc.bytex.transformer.processor.Output;

import java.io.IOException;
import java.util.List;

/**
 * Created by yangzhiqian on 2020-03-06<br/>
 */
public class IncrementalFileProcessor implements FileProcessor {
    private final List<MainProcessHandler> handlers;
    private final ClassFileProcessor classFileProcessor;

    public IncrementalFileProcessor(List<MainProcessHandler> handlers, ClassFileProcessor classFileProcessor) {
        this.handlers = handlers;
        this.classFileProcessor = classFileProcessor;
    }

    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        if (input.getFileData().getRelativePath().endsWith(".class")) {
            return classFileProcessor.process(chain);
        } else {
            for (MainProcessHandler handler : handlers) {
                handler.traverseIncremental(input.getFileData(), (ClassVisitorChain) null);
            }
            return chain.proceed(input);
        }
    }
}
