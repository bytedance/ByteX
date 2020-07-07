package com.ss.android.ugc.bytex.base;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.ByteXExtension;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import org.gradle.api.Project;

import java.io.File;

public class Context extends BaseContext<ByteXExtension> {
    private File buildDir;

    Context(Project project, AppExtension android, ByteXExtension extension) {
        super(project, android, extension);
        buildDir = new File(project.getBuildDir(), "ByteX");
    }

    public void init(TransformContext transformContext) {
        buildDir = transformContext.byteXBuildDir();
        super.init();
    }

    @Override
    public File buildDir() {
        return buildDir;
    }

    protected File getLoggerFile() {
        return new File(buildDir, extension.getLogFile());
    }

    @Override
    public synchronized void releaseContext() {
        super.releaseContext();
        buildDir = new File(project.getBuildDir(), "ByteX");
    }
}
