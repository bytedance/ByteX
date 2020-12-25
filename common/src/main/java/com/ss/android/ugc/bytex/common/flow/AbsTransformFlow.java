package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public abstract class AbsTransformFlow implements TransformFlow {
    private TransformFlow preTransformFlow = null;
    private TransformFlow nextTransformFlow = null;
    protected final TransformEngine transformEngine;
    protected final TransformContext context;

    public AbsTransformFlow(TransformEngine transformEngine) {
        this.transformEngine = transformEngine;
        this.context = transformEngine.getContext();
    }

    /**
     * use ${@link #markRunningState(TransformContext.State)} instead
     */
    @Deprecated
    protected void beginRun() {
        markRunningState(TransformContext.State.INITIALIZING);
    }

    /**
     * use ${@link #markRunningState(TransformContext.State)} instead
     */
    @Deprecated
    protected void running() {
        markRunningState(TransformContext.State.TRAVERSING);
    }

    /**
     * use ${@link #markRunningState(TransformContext.State)} instead
     */
    @Deprecated
    public void endRun() {
        markRunningState(TransformContext.State.STATELESS);
    }

    protected void markRunningState(TransformContext.State state) {
        transformEngine.markRunningState(state);
    }

    @Override
    public File getGraphCache() {
        return new File(context.byteXBuildDir(), "graphCache-" + name() + ".json");
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
        transformEngine.traverseAndroidJar(androidJar(), processors);
        return this;
    }

    protected File androidJar() throws FileNotFoundException {
        return context.androidJar();
    }

    protected AbsTransformFlow transform(FileProcessor... processors) throws IOException, InterruptedException {
        markRunningState(TransformContext.State.BEFORETRANSFORM);
        beforeTransform(transformEngine);
        markRunningState(TransformContext.State.TRANSFORMING);
        transform(transformEngine, this.nextTransformFlow == null, processors);
        markRunningState(TransformContext.State.AFTERTRANSFORM);
        afterTransform(transformEngine);
        return this;
    }

    protected abstract AbsTransformFlow beforeTransform(TransformEngine transformEngine) throws IOException;

    protected AbsTransformFlow transform(TransformEngine transformEngine, boolean isLast, FileProcessor... processors) throws IOException {
        transformEngine.transform(isLast, processors);
        return this;
    }

    protected abstract AbsTransformFlow afterTransform(TransformEngine transformEngine) throws IOException;

    @Override
    public void setPreTransformFlow(TransformFlow transformFlow) {
        this.preTransformFlow = transformFlow;
    }

    @Override
    public TransformFlow getPreTransformFlow() {
        return this.preTransformFlow;
    }

    @Override
    public void setNextTransformFlow(TransformFlow transformFlow) {
        this.nextTransformFlow = transformFlow;
    }

    @Override
    public TransformFlow getNextTransformFlow() {
        return this.nextTransformFlow;
    }

    @NotNull
    @Override
    public Iterator<TransformFlow> iterator() {
        return new Iterator<TransformFlow>() {
            TransformFlow current = AbsTransformFlow.this;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public TransformFlow next() {
                TransformFlow r = current;
                if (r == null) {
                    throw new IllegalStateException("There is no next TransformFlow");
                }
                current = current.getNextTransformFlow();
                return r;
            }
        };
    }
}
