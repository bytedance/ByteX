package com.ss.android.ugc.bytex.common.processor;

import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.transformer.processor.FileHandler;

import java.util.List;

abstract class MainProcessFileHandler implements FileHandler {
    List<MainProcessHandler> handlers;

    MainProcessFileHandler(List<MainProcessHandler> handlers) {
        this.handlers = handlers;
    }

    protected ClassVisitorChain getClassVisitorChain(String relativePath) {
        return new ClassVisitorChain();
    }
}
