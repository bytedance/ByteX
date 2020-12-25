package com.ss.android.ugc.bytex.refercheck;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.flow.TransformFlow;
import com.ss.android.ugc.bytex.common.flow.main.MainTransformFlow;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;
import com.ss.android.ugc.bytex.refercheck.log.ErrorLogGenerator;
import com.ss.android.ugc.bytex.refercheck.log.PinpointProblemAnalyzer;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferCheckClassVisitor;
import com.ss.android.ugc.bytex.transformer.TransformContext;
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
        chain.connect(new ReferCheckClassVisitor(context, context.getClassGraph()));
        return super.transform(relativePath, chain);
    }

    @Override
    public void afterTransform(@Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        ErrorLogGenerator errorLogGenerator = new ErrorLogGenerator(
                context.extension.moreErrorInfo() ? PinpointProblemAnalyzer.getPinpointProblemAnalyzer(project, context.getTransformContext().getVariantName(), context.getClassGraph()) : null,
                fileName -> Utils.getAllFileCachePath(context.getTransformContext(), fileName),
                context.getTransformContext().getVariantName(),
                context.extension.getOwner(),
                context.getInaccessibleNodes()
        );
        String msg = errorLogGenerator.generate();
        if (msg != null && !msg.isEmpty()) {
            if (extension.isStrictMode()) {
                context.getLogger().e(context.extension.getName(), msg, null);
                throw new RuntimeException(msg);
            } else {
                context.getLogger().e(context.extension.getName(), msg, null);
            }
        }
    }

    @Override
    protected TransformFlow provideTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
        return new MainTransformFlow(new TransformEngine(transformContext)) {
            @Override
            public int getPriority() {
                return Integer.MIN_VALUE;
            }
        }.appendHandler(this);
    }
}
