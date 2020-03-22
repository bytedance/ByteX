package com.ss.android.ugc.bytex.common.hook;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.ByteXExtension;
import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProxyTransform extends CommonTransform<BaseContext> {
    private List<IPlugin> plugins = new ArrayList<>();
    private final String transformName;
    private final Transform origTransform;

    ProxyTransform(Project project, AppExtension android, String transformName, Transform origTransform) {
        super(new BaseContext<ByteXExtension>(project, android, new ByteXExtension() {
            @Override
            public String getName() {
                return "hook";
            }
        }));
        this.transformName = transformName;
        this.origTransform = origTransform;
    }

    @Override
    protected TransformContext getTransformContext(TransformInvocation transformInvocation) {
        return new ProxyTransformContext(transformInvocation, context.getProject(), context.getAndroid(), isIncremental(), shouldSaveCache());
    }

    @Override
    protected void afterTransform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.afterTransform(transformInvocation);
        context.getLogger().i("Start to execute " + origTransform.getClass());
        origTransform.transform(transformInvocation);
        context.getLogger().i("Finish to execute " + origTransform.getClass());
    }

    @Override
    public String getName() {
        return transformName + "Hook";
    }

    void appendPlugin(IPlugin plugin) {
        plugins.add(plugin);
    }

    @Override
    protected List<IPlugin> getPlugins() {
        return plugins;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return origTransform.getInputTypes();
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return origTransform.getScopes();
    }

    private static class ProxyTransformContext extends TransformContext {

        ProxyTransformContext(TransformInvocation invocation, Project project, AppExtension android, boolean isIncremental, boolean shouldSaveCache) {
            super(invocation, project, android, isIncremental, shouldSaveCache, BooleanProperty.ENABLE_RAM_CACHE.value());
        }

        @Override
        public File getOutputFile(QualifiedContent content) throws IOException {
            return content.getFile();
        }

        @Override
        public File getOutputFile(QualifiedContent content, boolean createIfNeed) throws IOException {
            return content.getFile();
        }
    }
}
