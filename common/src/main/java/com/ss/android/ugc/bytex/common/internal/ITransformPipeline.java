package com.ss.android.ugc.bytex.common.internal;

import java.io.IOException;

public interface ITransformPipeline {

    void bind(final FlowBinder binder);

    void onPreTransform() throws IOException, InterruptedException;

    void runTransform() throws IOException, InterruptedException;

    void onPostTransform() throws IOException;

    void skipTransform() throws IOException;
}
