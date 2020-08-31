package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.common.flow.main.MainTransformFlow;
import com.ss.android.ugc.bytex.common.graph.Graph;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

public interface TransformFlow extends Iterable<TransformFlow>, TransformFlowLifecycleAware {

    default String name() {
        int flowIndex = 0;
        TransformFlow flow = this;
        while (flow.getPreTransformFlow() != null) {
            flowIndex++;
            flow = flow.getPreTransformFlow();
        }
        String name = this.getClass().getName();
        return name.substring(name.lastIndexOf(".") + 1) + "-" + flowIndex;
    }

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
     * 返回TransformFlow的Graph缓存文件路径，增量编译需要读取和写入使用
     * Returns the path of the Graph cache file of TransformFlow,witch is used by incremental compilation
     */
    File getGraphCache();

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
