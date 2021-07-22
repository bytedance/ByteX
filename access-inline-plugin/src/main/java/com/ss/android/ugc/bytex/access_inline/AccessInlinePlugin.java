package com.ss.android.ugc.bytex.access_inline;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.access_inline.visitor.PreProcessClassVisitor;
import com.ss.android.ugc.bytex.access_inline.visitor.ShrinkAccessClassVisitor;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.TransformConfiguration;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class AccessInlinePlugin extends CommonPlugin<AccessInlineExtension, Context> {
    @Override
    protected Transform getTransform() {
        return new InlineAccessTransform(context, this);
    }

    @Override
    protected Context getContext(Project project, AppExtension android, AccessInlineExtension extension) {
        return new Context(project, extension, android);
    }

    @Override
    public void traverse(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverse(relativePath, chain);
        if (!context.inWhiteList(Utils.getClassName(relativePath))) {
            chain.connect(new PreProcessClassVisitor(this.context));
        }
    }

    @Override
    public void traverseAndroidJar(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverseAndroidJar(relativePath, chain);
        if (!context.inWhiteList(Utils.getClassName(relativePath))) {
            chain.connect(new PreProcessClassVisitor(this.context, true));
        }
    }

    @Override
    public void beforeTransform(@NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        context.prepare();
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        chain.connect(new ShrinkAccessClassVisitor(this.context));
        return super.transform(relativePath, chain);
    }

    @Nonnull
    @Override
    public TransformConfiguration transformConfiguration() {
        return new TransformConfiguration() {
            @Override
            public boolean isIncremental() {
                return false;
            }
        };
    }
}
