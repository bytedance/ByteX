package com.ss.android.ugc.bytex.field_assign_opt;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.field_assign_opt.visitors.FieldAssignCollectClassVisitor;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@PluginConfig("bytex.field_assign_opt")
public class FieldAssignOptPlugin extends CommonPlugin<FieldAssignOptExtension, Context> {

    @Override
    protected Context getContext(Project project, AppExtension android, FieldAssignOptExtension extension) {
        return new Context(project, android, extension);
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        chain.connect(new FieldAssignCollectClassVisitor(context));
        return super.transform(relativePath, chain);
    }
}
