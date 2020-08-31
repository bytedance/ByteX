package com.ss.android.ugc.bytex.common.flow;

import com.ss.android.ugc.bytex.common.builder.internal.GlobalByteXBuildListener;
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 * Desc:
 */
public class TransformFlowListenerManager implements TransformFlowListener {
    private Set<TransformFlowListener> listeners = new HashSet<>();

    {
        listeners.add(GlobalByteXBuildListener.INSTANCE);
    }


    @Override
    public void startPrepare(@Nonnull TransformFlow transformFlow) {
        for (TransformFlowListener listener : getListeners()) {
            listener.startPrepare(transformFlow);
        }
    }

    @Override
    public void finishPrepare(@Nonnull TransformFlow transformFlow, @Nullable Exception exception) {
        for (TransformFlowListener listener : getListeners()) {
            listener.finishPrepare(transformFlow, exception);
        }
    }

    @Override
    public void startRunning(@Nonnull TransformFlow transformFlow, boolean isIncremental) {
        for (TransformFlowListener listener : getListeners()) {
            listener.startRunning(transformFlow, isIncremental);
        }
    }

    @Override
    public void finishRunning(@Nonnull TransformFlow transformFlow, @Nullable Exception exception) {
        for (TransformFlowListener listener : getListeners()) {
            listener.finishRunning(transformFlow, exception);
        }
    }

    @Override
    public void onAppendMainProcessHandler(@Nonnull TransformFlow transformFlow, MainProcessHandler handler) {
        for (TransformFlowListener listener : getListeners()) {
            listener.onAppendMainProcessHandler(transformFlow, handler);
        }
    }

    private synchronized List<TransformFlowListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    public synchronized void registerTransformFlowListener(TransformFlowListener listener) {
        listeners.add(listener);
    }

    public synchronized void unregisterTransformFlowListener(TransformFlowListener listener) {
        listeners.remove(listener);
    }
}
