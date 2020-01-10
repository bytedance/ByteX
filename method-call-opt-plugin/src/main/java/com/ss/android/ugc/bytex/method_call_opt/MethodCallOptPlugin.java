package com.ss.android.ugc.bytex.method_call_opt;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.method_call_opt.visitors.MethodCallClassVisitor;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@PluginConfig("bytex.method_call_opt")
public class MethodCallOptPlugin extends CommonPlugin<MethodCallOptExtension, MethodCallOptContext> {
    @Override
    protected MethodCallOptContext getContext(Project project, AppExtension android, MethodCallOptExtension extension) {
        return new MethodCallOptContext(project, android, extension);
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        if (!context.isOptimizationNeededMethodsEmpty() && context.needCheckClass(relativePath.substring(0, relativePath.lastIndexOf('.')))) {
            chain.connect(new MethodCallClassVisitor(context));
        }
        return super.transform(relativePath, chain);
    }

    @Override
    public void afterTransform(@NotNull @Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        context.release();
    }
}
