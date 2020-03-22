package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.io.IOException;

public abstract class AbsTransformFlow implements TransformFlow {
    protected final TransformEngine transformEngine;
    protected final TransformContext context;
    private boolean isLast;

    public AbsTransformFlow(TransformEngine transformEngine) {
        this.transformEngine = transformEngine;
        this.context = transformEngine.getContext();
    }

    protected void beginRun() {
        transformEngine.beginRun();
    }

    protected void running() {
        transformEngine.running();
    }

    protected AbsTransformFlow traverse(FileProcessor... processors) throws IOException, InterruptedException {
        traverseArtifactOnly(processors);
        traverseAndroidJarOnly(processors);
        return this;
    }

    protected AbsTransformFlow traverseArtifactOnly(FileProcessor... processors) throws IOException, InterruptedException {
        transformEngine.traverseOnly(processors);
        return this;
    }

    protected AbsTransformFlow traverseAndroidJarOnly(FileProcessor... processors) throws IOException, InterruptedException {
        transformEngine.traverseAndroidJar(context.androidJar(), processors);
        return this;
    }

    protected AbsTransformFlow transform(FileProcessor... processors) throws IOException, InterruptedException {
        beforeTransform(transformEngine);
        transformEngine.transform(isLast, processors);
        afterTransform(transformEngine);
        return this;
    }

    protected abstract AbsTransformFlow beforeTransform(TransformEngine transformEngine);

    protected abstract AbsTransformFlow afterTransform(TransformEngine transformEngine);

    public void endRun() {
        transformEngine.endRun();
    }

    @Override
    public final TransformFlow asTail() {
        isLast = true;
        return this;
    }
}
