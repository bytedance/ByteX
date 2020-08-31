package com.ss.android.ugc.bytex.common.flow;

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 */
interface TransformFlowLifecycleAware {
    default void registerTransformFlowListener(TransformFlowListener listener) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(getClass().getName() + " Does Not Support TransformFlowListener");
    }

    default boolean isLifecycleAware() {
        return false;
    }
}
