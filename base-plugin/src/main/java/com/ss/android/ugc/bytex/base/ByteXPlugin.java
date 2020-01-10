package com.ss.android.ugc.bytex.base;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.ByteXExtension;
import com.ss.android.ugc.bytex.gradletoolkit.Project_;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class ByteXPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project project) {
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        ByteXExtension extension = project.getExtensions().create("ByteX", ByteXExtension.class);
        android.registerTransform(new ByteXTransform(new Context(project, android, extension)));
        Project_.assembleVariantProcessor(project);
    }
}
