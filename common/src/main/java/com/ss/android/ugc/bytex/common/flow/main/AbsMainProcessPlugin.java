package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.common.AbsPlugin;
import com.ss.android.ugc.bytex.common.BaseExtension;
import com.ss.android.ugc.bytex.common.IPlugin;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.processor.CommonFileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public abstract class AbsMainProcessPlugin<E extends BaseExtension> extends AbsPlugin<E> implements MainProcessHandler {
    private TransformFlow transformFlow;
    private Processor[] processorAnnotations = getClass().getAnnotationsByType(Processor.class);
    private Handler[] handlerAnnotations = getClass().getAnnotationsByType(Handler.class);


    @Override
    public void init() {
    }

    @Override
    public void traverse(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain) {
    }

    @Override
    public void traverseAndroidJar(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain) {
    }

    @Override
    public void traverse(@Nonnull String relativePath, @Nonnull ClassNode node) {

    }

    @Override
    public void traverseAndroidJar(@Nonnull String relativePath, @Nonnull ClassNode node) {

    }

    @Override
    public void beforeTransform(@Nonnull TransformEngine engine) {
    }

    @Override
    public boolean transform(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain) {
        return true;
    }

    @Override
    public boolean transform(@Nonnull String relativePath, @Nonnull ClassNode node) {
        return true;
    }

    @Override
    public void afterTransform(@Nonnull TransformEngine engine) {
    }


    /**
     * Each ByteX plugin can have its own TransformFlow, and the return
     * value of this method determines which flow the plugin runs in.<br/>
     * We mark it as final because TransformFlow should register only once<br/>
     */
    @Nonnull
    @Override
    public final TransformFlow registerTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
        if (transformFlow == null) {
            transformFlow = provideTransformFlow(mainFlow, transformContext);
            if (transformFlow == null) {
                throw new RuntimeException("TransformFlow can not be null.");
            }
        }
        return transformFlow;
    }

    @Override
    public final TransformFlow getTransformFlow() {
        return transformFlow;
    }

    /**
     * create a new transformFlow or just return mainFlow and append a handler.
     * It will be called by {@link IPlugin#registerTransformFlow(MainTransformFlow, TransformContext)} when
     * handle start.
     *
     * @param mainFlow         main TransformFlow
     * @param transformContext handle context
     * @return return a new TransformFlow object if you want make a new flow for current plugin
     */
    protected TransformFlow provideTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
        return mainFlow.appendHandler(this);
    }

    @Override
    public List<FileProcessor> process(Process process) {
        try {
            List<FileProcessor> fileProcessors = new ArrayList<>();
            for (Processor annotation : processorAnnotations) {
                if (process == annotation.process()) {
                    fileProcessors.add(annotation.implement().newInstance());
                }
            }
            for (Handler annotation : handlerAnnotations) {
                if (process == annotation.process()) {
                    fileProcessors.add(CommonFileProcessor.newInstance(annotation.implement().newInstance()));
                }
            }
            return fileProcessors;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


    @Override
    public boolean needPreVerify() {
        return extension.isNeedPreVerify();
    }

    @Override
    public boolean needVerify() {
        return extension.isNeedVerify();
    }

    @Override
    public void afterExecute() throws Throwable {
        super.afterExecute();
        transformFlow = null;
    }
}
