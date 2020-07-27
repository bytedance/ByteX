package com.ss.android.ugc.bytex.proguardconfigurationresolver;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import javax.annotation.Nullable;

/**
 * Created by yangzhiqian on 2020/7/6<br/>
 */
public abstract class ProguardConfigurationResolver {
    protected Project project;
    protected String variantName;

    public ProguardConfigurationResolver(Project project, String variantName) {
        this.project = project;
        this.variantName = variantName;
    }

    @Nullable
    public abstract Task getTask();

    @Nullable
    public abstract FileCollection getAllConfigurationFiles();
}

