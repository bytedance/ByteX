package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.transformer.TransformEngine;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */
public interface MainProcessHandlerListener {
    void startInit(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer);

    void finishInit(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer, @Nullable Exception exception);

    void startTraverseIncremental(@Nonnull Collection<MainProcessHandler> handlers);

    void finishTraverseIncremental(@Nonnull Collection<MainProcessHandler> handlers, @Nullable Exception exception);

    void startBeforeTraverse(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer);

    void finishBeforeTraverse(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer, @Nullable Exception exception);

    void startStartRunning(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer);

    void finishStartRunning(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer, @Nullable Exception exception);

    void startTraverse(@Nonnull Collection<MainProcessHandler> handlers);

    void finishTraverse(@Nonnull Collection<MainProcessHandler> handlers, @Nullable Exception exception);

    void startTraverseAndroidJar(@Nonnull Collection<MainProcessHandler> handlers);

    void finishTraverseAndroidJar(@Nonnull Collection<MainProcessHandler> handlers, @Nullable Exception exception);

    void startBeforeTransform(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer);

    void finishBeforeTransform(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer, @Nullable Exception exception);

    void startTransform(@Nonnull Collection<MainProcessHandler> handlers);

    void finishTransform(@Nonnull Collection<MainProcessHandler> handlers, @Nullable Exception exception);

    void startAfterTransform(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer);

    void finishAfterTransform(@Nonnull MainProcessHandler handler, @Nonnull TransformEngine transformer, @Nullable Exception exception);
}
