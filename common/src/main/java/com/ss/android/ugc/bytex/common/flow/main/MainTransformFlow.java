package com.ss.android.ugc.bytex.common.flow.main;

import com.android.build.api.transform.Status;
import com.ss.android.ugc.bytex.common.flow.AbsTransformFlow;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.flow.TransformFlowListener;
import com.ss.android.ugc.bytex.common.flow.TransformFlowListenerManager;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.GraphBuilder;
import com.ss.android.ugc.bytex.common.graph.cache.CachedGraphBuilder;
import com.ss.android.ugc.bytex.common.log.Timer;
import com.ss.android.ugc.bytex.common.processor.ClassFileAnalyzer;
import com.ss.android.ugc.bytex.common.processor.ClassFileTransformer;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers;
import com.ss.android.ugc.bytex.transformer.processor.ClassFileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.FileHandler;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.FilterFileProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class MainTransformFlow extends AbsTransformFlow {
    private final MainProcessHandlerContainer handlers = new MainProcessHandlerContainer();
    private Graph mClassGraph;
    private TransformFlowListenerManager listenerManager = new TransformFlowListenerManager();
    private Timer timer = new Timer();

    public MainTransformFlow(TransformEngine transformEngine) {
        super(transformEngine);
    }

    @Override
    public void prepare() throws IOException, InterruptedException {
        try {
            listenerManager.startPrepare(this);
            prepareInternal();
            listenerManager.finishPrepare(this, null);
        } catch (Exception e) {
            listenerManager.finishPrepare(this, e);
            throw e;
        }
    }

    private void prepareInternal() throws IOException, InterruptedException {
        super.prepare();
        markRunningState(TransformContext.State.INITIALIZING);
        timer.startRecord("INIT");
        Schedulers.COMPUTATION().submitAndAwait(handlers, handler -> handler.init(transformEngine));
        timer.stopRecord("INIT", "Process init cost time = [%s ms]");
        markRunningState(TransformContext.State.INITIALIZED);
        if (!isOnePassEnough()) {
            markRunningState(TransformContext.State.INCREMENTALTRAVERSING);
            if (context.isIncremental()) {
                try {
                    GlobalMainProcessHandlerListener.INSTANCE.startTraverseIncremental(handlers);
                    timer.startRecord("TRAVERSE_INCREMENTAL");
                    traverseArtifactOnly(getProcessors(Process.TRAVERSE_INCREMENTAL, new ClassFileAnalyzer(context, Process.TRAVERSE_INCREMENTAL, null, new ArrayList<>(handlers))));
                    timer.stopRecord("TRAVERSE_INCREMENTAL", "Process project all .class files cost time = [%s ms]");
                    GlobalMainProcessHandlerListener.INSTANCE.finishTraverseIncremental(handlers, null);
                } catch (Exception e) {
                    GlobalMainProcessHandlerListener.INSTANCE.finishTraverseIncremental(handlers, e);
                    throw e;
                }
            }
            markRunningState(TransformContext.State.BEFORETRAVERSE);
            Schedulers.COMPUTATION().submitAndAwait(handlers, plugin -> plugin.beforeTraverse(transformEngine));
        }
    }

    @Override
    public void run() throws IOException, InterruptedException {
        try {
            listenerManager.startRunning(this, context.isIncremental());
            runTransform();
            listenerManager.finishRunning(this, null);
        } catch (Exception e) {
            listenerManager.finishRunning(this, e);
            throw e;
        } finally {
            markRunningState(TransformContext.State.STATELESS);
        }
    }

    private void runTransform() throws IOException, InterruptedException {
        markRunningState(TransformContext.State.RUNNING);
        if (handlers.isEmpty()) return;
        timer.startRecord("PRE_PROCESS");
        Schedulers.COMPUTATION().submitAndAwait(handlers, plugin -> plugin.startRunning(transformEngine));
        if (!isOnePassEnough()) {
            timer.startRecord("LOADCACHE");
            GraphBuilder graphBuilder = new CachedGraphBuilder(getGraphCache(), context.isIncremental(), context.shouldSaveCache(), !context.isDaemonSingleUse());
            if (context.isIncremental() && !graphBuilder.isCacheValid()) {
                throw new IllegalStateException("Transform is running as incrementally, but failed to load cache for the transform!");
            }
            timer.stopRecord("LOADCACHE", "Process loading cache cost time = [%s ms]");
            markRunningState(TransformContext.State.TRANSFORMING);
            try {
                GlobalMainProcessHandlerListener.INSTANCE.startTraverse(handlers);
                timer.startRecord("PROJECT_CLASS");
                traverseArtifactOnly(getProcessors(Process.TRAVERSE, new ClassFileAnalyzer(context, Process.TRAVERSE, graphBuilder, new ArrayList<>(handlers))));
                timer.stopRecord("PROJECT_CLASS", "Process project all .class files cost time = [%s ms]");
                GlobalMainProcessHandlerListener.INSTANCE.finishTraverse(handlers, null);
            } catch (Exception e) {
                GlobalMainProcessHandlerListener.INSTANCE.finishTraverse(handlers, e);
                throw e;
            }

            try {
                GlobalMainProcessHandlerListener.INSTANCE.startTraverseAndroidJar(handlers);
                timer.startRecord("ANDROID");
                markRunningState(TransformContext.State.TRAVERSINGANDROID);
                traverseAndroidJarOnly(getProcessors(Process.TRAVERSE_ANDROID, new ClassFileAnalyzer(context, Process.TRAVERSE_ANDROID, graphBuilder, new ArrayList<>(handlers))));
                GlobalMainProcessHandlerListener.INSTANCE.finishTraverseAndroidJar(handlers, null);
            } catch (Exception e) {
                GlobalMainProcessHandlerListener.INSTANCE.finishTraverseAndroidJar(handlers, e);
                throw e;
            }
            timer.stopRecord("ANDROID", "Process android jar cost time = [%s ms]");
            timer.startRecord("SAVECACHE");
            mClassGraph = graphBuilder.build();
            timer.stopRecord("SAVECACHE", "Process saving cache cost time = [%s ms]");
        }

        GlobalMainProcessHandlerListener.INSTANCE.startTransform(handlers);
        timer.stopRecord("PRE_PROCESS", "Collect info cost time = [%s ms]");
        timer.startRecord("PROCESS");
        transform(getProcessors(Process.TRANSFORM, new ClassFileTransformer(new ArrayList<>(handlers), needPreVerify(), needVerify())));
        timer.stopRecord("PROCESS", "Transform cost time = [%s ms]");
    }

    private boolean isOnePassEnough() {
        return handlers.stream().allMatch(MainProcessHandler::isOnePassEnough);
    }

    private FileProcessor[] getProcessors(Process process, FileHandler fileHandler) {
        List<FileProcessor> processors = handlers.stream()
                .flatMap((Function<MainProcessHandler, Stream<FileProcessor>>) handler -> handler.process(process).stream())
                .collect(Collectors.toList());
        switch (process) {
            case TRAVERSE_INCREMENTAL:
                processors.add(0, new FilterFileProcessor(fileData -> fileData.getStatus() != Status.NOTCHANGED));
                processors.add(new IncrementalFileProcessor(new ArrayList<>(handlers), ClassFileProcessor.newInstance(fileHandler)));
                break;
            case TRAVERSE:
            case TRAVERSE_ANDROID:
            case TRANSFORM:
                processors.add(ClassFileProcessor.newInstance(fileHandler));
                processors.add(0, new FilterFileProcessor(fileData -> fileData.getStatus() != Status.NOTCHANGED && fileData.getStatus() != Status.REMOVED));
                break;
            default:
                throw new RuntimeException("Unknow Process:" + process);
        }
        return processors.toArray(new FileProcessor[0]);
    }

    private boolean needPreVerify() {
        for (MainProcessHandler handler : handlers) {
            if (handler.needPreVerify()) {
                return true;
            }
        }
        return false;
    }

    private boolean needVerify() {
        for (MainProcessHandler handler : handlers) {
            if (handler.needVerify()) {
                return true;
            }
        }
        return false;
    }

    public final TransformFlow appendHandler(MainProcessHandler handler) {
        handlers.add(handler);
        listenerManager.onAppendMainProcessHandler(this, handler);
        return this;
    }

    @Override
    protected AbsTransformFlow beforeTransform(TransformEngine transformEngine) throws IOException {
        Schedulers.COMPUTATION().submitAndAwait(handlers, plugin -> plugin.beforeTransform(transformEngine));
        return this;
    }

    @Override
    protected AbsTransformFlow transform(TransformEngine transformEngine, boolean isLast, FileProcessor... processors) throws IOException {
        try {
            GlobalMainProcessHandlerListener.INSTANCE.startTransform(handlers);
            AbsTransformFlow result = super.transform(transformEngine, isLast, processors);
            GlobalMainProcessHandlerListener.INSTANCE.finishTransform(handlers, null);
            return result;
        } catch (Exception e) {
            GlobalMainProcessHandlerListener.INSTANCE.finishTransform(handlers, e);
            throw e;
        }
    }

    @Override
    protected AbsTransformFlow afterTransform(TransformEngine transformEngine) throws IOException {
        Schedulers.COMPUTATION().submitAndAwait(handlers, plugin -> plugin.afterTransform(transformEngine));
        return this;
    }

    @Nullable
    @Override
    public Graph getClassGraph() {
        return mClassGraph;
    }

    @Override
    public void registerTransformFlowListener(TransformFlowListener listener) throws UnsupportedOperationException {
        listenerManager.registerTransformFlowListener(listener);
    }

    @Override
    public boolean isLifecycleAware() {
        return true;
    }
}
