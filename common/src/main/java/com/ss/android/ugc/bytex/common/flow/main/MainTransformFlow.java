package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.common.flow.AbsTransformFlow;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.GraphBuilder;
import com.ss.android.ugc.bytex.common.graph.cache.CachedGraphBuilder;
import com.ss.android.ugc.bytex.common.log.Timer;
import com.ss.android.ugc.bytex.common.processor.ClassFileAnalyzer;
import com.ss.android.ugc.bytex.common.processor.ClassFileTransformer;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.processor.ClassFileProcessor;
import com.ss.android.ugc.bytex.transformer.processor.FileHandler;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class MainTransformFlow extends AbsTransformFlow {
    private final List<MainProcessHandler> handlers;
    private Graph mClassGraph;

    public MainTransformFlow(TransformEngine transformEngine) {
        super(transformEngine);
        this.handlers = new ArrayList<>();
    }

    @Override
    public void run() throws IOException, InterruptedException {
        try {
            beginRun();
            runTransform();
        } finally {
            endRun();
        }
    }

    private void runTransform() throws IOException, InterruptedException {
        if (handlers.isEmpty()) return;
        Timer timer = new Timer();
        timer.startRecord("PRE_PROCESS");
        timer.startRecord("INIT");
        for (MainProcessHandler handler : handlers) {
            handler.init(transformEngine);
        }
        timer.stopRecord("INIT", "Process init cost time = [%s ms]");
        if (!isOnePassEnough()) {
            timer.startRecord("LOADCACHE");
            GraphBuilder graphBuilder = new CachedGraphBuilder(context.getGraphCache(), context.isIncremental(), context.shouldSaveCache());
            if (!graphBuilder.isCacheValid()) {
                context.requestNotIncremental();
            }
            timer.stopRecord("LOADCACHE", "Process loading cache cost time = [%s ms]");
            if (!handlers.isEmpty()) {
                timer.startRecord("PROJECT_CLASS");
                traverseArtifactOnly(getProcessors(Process.TRAVERSE, new ClassFileAnalyzer(context, false, graphBuilder, handlers)));
                timer.stopRecord("PROJECT_CLASS", "Process project all .class files cost time = [%s ms]");
            }

            if (!handlers.isEmpty()) {
                timer.startRecord("ANDROID");
                traverseAndroidJarOnly(getProcessors(Process.TRAVERSE_ANDROID, new ClassFileAnalyzer(context, true, graphBuilder, handlers)));
                timer.stopRecord("ANDROID", "Process android jar cost time = [%s ms]");
            }
            timer.startRecord("SAVECACHE");
            mClassGraph = graphBuilder.build();
            timer.stopRecord("SAVECACHE", "Process saving cache cost time = [%s ms]");
        }

        timer.stopRecord("PRE_PROCESS", "Collect info cost time = [%s ms]");

        if (!handlers.isEmpty()) {
            timer.startRecord("PROCESS");
            transform(getProcessors(Process.TRANSFORM, new ClassFileTransformer(handlers, needPreVerify(), needVerify())));
            timer.stopRecord("PROCESS", "Transform cost time = [%s ms]");
        }
    }

    private boolean isOnePassEnough() {
        return handlers.stream().allMatch(MainProcessHandler::isOnePassEnough);
    }

    private FileProcessor[] getProcessors(Process process, FileHandler fileHandler) {
        List<FileProcessor> processors = handlers.stream()
                .flatMap((Function<MainProcessHandler, Stream<FileProcessor>>) handler -> handler.process(process).stream())
                .collect(Collectors.toList());
        processors.add(ClassFileProcessor.newInstance(fileHandler));
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
        return this;
    }

    @Override
    protected AbsTransformFlow beforeTransform(TransformEngine transformEngine) {
        handlers.forEach(plugin -> plugin.beforeTransform(transformEngine));
        return this;
    }

    @Override
    protected AbsTransformFlow afterTransform(TransformEngine transformEngine) {
        handlers.forEach(plugin -> plugin.afterTransform(transformEngine));
        return this;
    }

    @Nullable
    @Override
    public Graph getClassGraph() {
        return mClassGraph;
    }
}
