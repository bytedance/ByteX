package com.ss.android.ugc.bytex.serialization_check;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.serialization_check.visitor.SerializationCheckClassVisitor;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class SerializationCheckPlugin extends CommonPlugin<SerializationCheckExtension, Context> {

    @Override
    protected Context getContext(Project project, AppExtension android, SerializationCheckExtension extension) {
        return new Context(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        context.initWithWhiteList(extension.getWhiteList());
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        if (extension.getOnlyCheck().isEmpty() || extension.getOnlyCheck().stream().anyMatch(relativePath::startsWith)) {
            chain.connect(new SerializationCheckClassVisitor(context));
        }
        return super.transform(relativePath, chain);
    }


    @Override
    public void afterTransform(@NotNull TransformEngine engine) {
        super.afterTransform(engine);
        String error = context.outputError();
        if (error != null) {
            throw new RuntimeException(error);
        }
    }
}
