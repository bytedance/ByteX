package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.annotations.Beta;

/**
 * Created by yangzhiqian on 2020/8/25<br/>
 */
public interface TransformFlowListener {
    @Beta
    void onAppendMainProcessHandler(TransformFlow transformFlow, MainProcessHandler handler);

    void startPrepare(@Nonnull TransformFlow transformFlow);

    void finishPrepare(@Nonnull TransformFlow transformFlow, @Nullable Exception exception);

    void startRunning(@Nonnull TransformFlow transformFlow, boolean isIncremental);

    void finishRunning(@Nonnull TransformFlow transformFlow, @Nullable Exception exception);
}
