package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.common.flow.main.MainTransformFlow;
import com.ss.android.ugc.bytex.common.graph.Graph;

import java.io.IOException;

import javax.annotation.Nullable;

public interface TransformFlow extends Iterable<TransformFlow> {
    /**
     * internal only
     */
    default void prepare() throws IOException, InterruptedException {
    }

    /**
     * internal only
     */
    void run() throws IOException, InterruptedException;

    /**
     * Every TransformFlow(such as {@link MainTransformFlow}.But it needs to be generated after t
     * he traverse method is called) has the possibility to build a class diagram
     *
     * @return Graph. Maybe return null if current transformFlow does not generate the class diagram
     * or class diagram has not been generated yet
     */
    default @Nullable
    Graph getClassGraph() {
        return null;
    }

    /**
     * internal only
     */
    void setPreTransformFlow(TransformFlow transformFlow);

    @Nullable
    TransformFlow getPreTransformFlow();

    /**
     * internal only
     */
    void setNextTransformFlow(TransformFlow transformFlow);

    @Nullable
    TransformFlow getNextTransformFlow();


    /**
     * As a ByteX Plugin，it can run as a single Transform（With single TransformFlow）.
     * Universally， multiple bytex plugins run together in a single Transform. However,
     * there are some plugins or several plugins that require separate running in a single
     * TransformFlow ，so , we will collect and sort all the TransformFlows before running
     *
     * @return TransformFlow's priority.default is 0.If the priorities are the same,
     * we will sort them in the order of apply
     */
    default int getPriority() {
        return 0;
    }
}
