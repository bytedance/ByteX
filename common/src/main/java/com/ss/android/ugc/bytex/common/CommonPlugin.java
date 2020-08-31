package com.ss.android.ugc.bytex.common;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.flow.main.AbsMainProcessPlugin;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public abstract class CommonPlugin<E extends BaseExtension, X extends BaseContext> extends AbsMainProcessPlugin<E> {
    protected X context;

    protected abstract X getContext(Project project, AppExtension android, E extension);

    @Override
    public void startExecute(TransformContext transformContext) {
        super.startExecute(transformContext);
        context.setTransformContext(transformContext);
    }

    @Override
    public void init() {
        super.init();
        context.init();
    }

    @Override
    protected Transform getTransform() {
        return new SimpleTransform<>(context, this);
    }

    @Override
    public void beforeTransform(@NotNull @Nonnull TransformEngine engine) {
        super.beforeTransform(engine);
        context.setClassGraph(getTransformFlow().getClassGraph());
    }

    @Override
    protected void onApply(@NotNull @Nonnull Project project) {
        super.onApply(project);
        context = getContext(project, android, extension);
    }

    @Override
    public void afterExecute() throws Throwable {
        super.afterExecute();
        context.releaseContext();
    }
}
