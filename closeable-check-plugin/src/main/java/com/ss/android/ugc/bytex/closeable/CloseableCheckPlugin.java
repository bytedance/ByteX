package com.ss.android.ugc.bytex.closeable;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.closeable.visitors.CloseableCheckClassVisitor2;
import com.ss.android.ugc.bytex.closeable.visitors.CloseableCheckPreviewClassVisitor;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.flow.main.MainTransformFlow;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@PluginConfig("bytex.closeable_checker")
public class CloseableCheckPlugin extends CommonPlugin<CloseableCheckExtension, CloseableCheckContext> {
    @Override
    protected CloseableCheckContext getContext(Project project, AppExtension android, CloseableCheckExtension extension) {
        return new CloseableCheckContext(project, android, extension);
    }

    @Override
    public void traverse(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverse(relativePath, chain);
        chain.connect(new CloseableCheckPreviewClassVisitor(context));
    }

    @Override
    public void traverseAndroidJar(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverseAndroidJar(relativePath, chain);
        chain.connect(new CloseableCheckPreviewClassVisitor(context));
    }

    @Override
    public void beforeTransform(@NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        context.prepare();
        context.getLogger().d("empty closeables", context.getEmptyCloseables().toString());
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        boolean res = super.transform(relativePath, chain);
        chain.connect(new CloseableCheckClassVisitor2(context));
        return res;
    }

    @Override
    public void afterTransform(@NotNull @Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        context.release();
    }

    @Override
    protected TransformFlow provideTransformFlow(@NotNull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
        return new MainTransformFlow(new TransformEngine(transformContext)).appendHandler(this);
    }
}
