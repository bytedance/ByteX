package com.ss.android.ugc.bytex.gradletoolkit;

import com.android.build.api.transform.TransformInvocation;

/**
 * Created by tanlehua on 2019-04-29.
 */
public interface TransformEnv extends GradleEnv {
    void setTransformInvocation(TransformInvocation invocation);
}
