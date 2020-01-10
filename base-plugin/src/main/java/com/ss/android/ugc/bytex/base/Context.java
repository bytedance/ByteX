package com.ss.android.ugc.bytex.base;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.ByteXExtension;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.gradle.api.Project;

import java.io.File;

public class Context extends BaseContext<ByteXExtension> {
    public Context(Project project, AppExtension android, ByteXExtension extension) {
        super(project, android, extension);
    }

    public boolean isEnable() {
        return extension.isEnable();
    }

    @Override
    public File buildDir() {
        return new File(project.getBuildDir(), "ByteX");
    }
}
