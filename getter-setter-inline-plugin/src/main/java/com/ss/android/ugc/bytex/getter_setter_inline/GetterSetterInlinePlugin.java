package com.ss.android.ugc.bytex.getter_setter_inline;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.getter_setter_inline.visitor.FindGetterSetterClassVisitor;
import com.ss.android.ugc.bytex.getter_setter_inline.visitor.InlineGetterSetterClassVisitor;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class GetterSetterInlinePlugin extends CommonPlugin<GetterSettingInlineExtension, Context> {
    @Override
    protected Context getContext(Project project, AppExtension android, GetterSettingInlineExtension extension) {
        return new Context(project, android, extension);
    }

    @Override
    public void traverse(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverse(relativePath, chain);
        chain.connect(new FindGetterSetterClassVisitor(context));
    }

    @Override
    public void beforeTransform(@NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        // filter getters and setters that can't be inlined.
        // confirm real getter or setter target field.
        context.prepare();
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        chain.connect(new InlineGetterSetterClassVisitor(context));
        return super.transform(relativePath, chain);
    }

    @Override
    protected void onApply(@NotNull Project project) {
        super.onApply(project);
        context.hookProguard(project);
    }
}
