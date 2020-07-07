package com.ss.android.ugc.bytex.common.internal;

import com.android.annotations.NonNull;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.flow.main.MainTransformFlow;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import java.io.IOException;

public class TransformFlowerManager implements ITransformPipeline {
    private final MainTransformFlow commonFlow;
    private final TransformEngine engine;
    private TransformFlow first;

    public TransformFlowerManager(TransformContext context) {
        this.engine = new TransformEngine(context);
        this.commonFlow = new MainTransformFlow(engine);
    }

    @Override
    public void bind(final @NonNull FlowBinder binder) {
        final TransformFlow flow = binder.bind(this);
        if (first == null) {
            first = flow;
            return;
        }
        if (first.getPriority() < flow.getPriority()) {
            first.setPreTransformFlow(flow);
            flow.setNextTransformFlow(first);
            first = flow;
        } else {
            TransformFlow find = first;
            for (TransformFlow transformFlow : first) {
                if (transformFlow == flow) {
                    return;
                }
                TransformFlow next = transformFlow.getNextTransformFlow();
                if (next == null || next.getPriority() < flow.getPriority()) {
                    break;
                }
                find = transformFlow;
            }
            TransformFlow findNext = find.getNextTransformFlow();
            flow.setPreTransformFlow(find);
            flow.setNextTransformFlow(findNext);
            if (findNext != null) {
                findNext.setPreTransformFlow(flow);
            }
            find.setNextTransformFlow(flow);

        }
    }

    public MainTransformFlow getCommonFlow() {
        return commonFlow;
    }

    @Override
    public void onPreTransform() throws IOException, InterruptedException {
        for (TransformFlow flow : first) {
            flow.prepare();
        }
    }

    @Override
    public void runTransform() throws IOException, InterruptedException {
        for (TransformFlow flow : first) {
            flow.run();
            Graph graph = flow.getClassGraph();
            if (graph != null) {
                //clear the class diagram.we won’t use it anymore
                graph.clear();
            }
        }
    }

    @Override
    public void onPostTransform() throws IOException {
        engine.transformOutput();
    }

    @Override
    public void skipTransform() throws IOException {
        engine.skip();
    }

}
