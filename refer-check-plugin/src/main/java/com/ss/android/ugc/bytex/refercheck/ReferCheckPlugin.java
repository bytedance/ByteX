package com.ss.android.ugc.bytex.refercheck;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;
import com.ss.android.ugc.bytex.refercheck.log.ErrorLogGenerator;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferCheckClassVisitor;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;

import javax.annotation.Nonnull;

@PluginConfig("bytex.refer_check")
public class ReferCheckPlugin extends CommonPlugin<ReferCheckExtension, ReferCheckContext> {

    @Override
    protected ReferCheckContext getContext(Project project, AppExtension android, ReferCheckExtension extension) {
        return new ReferCheckContext(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        context.prepare();
    }

    @Override
    public boolean transform(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain) {
        chain.connect(new ReferCheckClassVisitor(context));
        return super.transform(relativePath, chain);
    }

    @Override
    public void afterTransform(@Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        ErrorLogGenerator errorLogGenerator = new ErrorLogGenerator(context, engine, project);
        String msg = errorLogGenerator.generate();
        if (msg != null && !msg.isEmpty()) {
            if (extension.isStrictMode()) {
                context.getLogger().e(context.extension.getName(), msg, null);
                throw new RuntimeException(msg);
            } else {
                context.getLogger().e(context.extension.getName(), msg, null);
            }
        }
        context.release();
    }
}
