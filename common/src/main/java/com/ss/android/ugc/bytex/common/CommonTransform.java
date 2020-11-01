package com.ss.android.ugc.bytex.common;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.SecondaryFile;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.variant.VariantInfo;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.internal.scope.VariantScope;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ss.android.ugc.bytex.common.builder.internal.GlobalByteXBuildListener;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.common.internal.ITransformPipeline;
import com.ss.android.ugc.bytex.common.internal.TransformFlowerManager;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.log.Timer;
import com.ss.android.ugc.bytex.common.log.html.HtmlReporter;
import com.ss.android.ugc.bytex.gradletoolkit.TransformInvocationKt;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Created by tlh on 2018/8/29.
 */

public abstract class CommonTransform<X extends BaseContext> extends Transform {
    protected final X context;
    private Set<TransformConfiguration> configurations;
    @Nullable
    //3.4在配置阶段才有
    private String applyingVariantName = null;

    public CommonTransform(X context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return context.extension.getName();
    }

    @Override
    public final boolean applyToVariant(VariantInfo variant) {
        applyingVariantName = variant.getFullVariantName();
        return super.applyToVariant(variant);
    }

    @Nullable
    private VariantScope getApplyingVariantScope() {
        return context.project.getPlugins().findPlugin(AppPlugin.class).getVariantManager().getVariantScopes().stream().filter(scope -> scope.getFullVariantName().equals(applyingVariantName)).findFirst().orElse(null);
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        Set<QualifiedContent.ContentType> result = ImmutableSet.of();
        for (TransformConfiguration config : getConfigurations()) {
            Set<QualifiedContent.ContentType> inputTypes = config.getInputTypes();
            if (!result.containsAll(inputTypes)) {
                result = Sets.union(result, inputTypes);
            }
        }
        if (result.isEmpty()) {
            return TransformConfiguration.DEFAULT.getInputTypes();
        }
        return result;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        Set<? super QualifiedContent.Scope> result = ImmutableSet.of();
        VariantScope variantScope = getApplyingVariantScope();
        for (TransformConfiguration config : getConfigurations()) {
            Set<? super QualifiedContent.Scope> scopes = config.getScopes(variantScope);
            if (!result.containsAll(scopes)) {
                result = Sets.union(result, scopes);
            }
        }
        if (result.isEmpty()) {
            return TransformConfiguration.DEFAULT.getScopes(variantScope);
        }
        return result;
    }

    @Override
    public Set<QualifiedContent.ContentType> getOutputTypes() {
        Set<QualifiedContent.ContentType> result = super.getOutputTypes();
        for (TransformConfiguration config : getConfigurations()) {
            Set<QualifiedContent.ContentType> outputTypes = config.getOutputTypes();
            if (!result.containsAll(outputTypes)) {
                result = Sets.union(result, outputTypes);
            }
        }
        if (result.isEmpty()) {
            return TransformConfiguration.DEFAULT.getOutputTypes();
        }
        return result;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getReferencedScopes() {
        Set<? super QualifiedContent.Scope> result = super.getReferencedScopes();
        VariantScope variantScope = getApplyingVariantScope();
        for (TransformConfiguration config : getConfigurations()) {
            Set<? super QualifiedContent.Scope> referencedScopes = config.getReferencedScopes(variantScope);
            if (!result.containsAll(referencedScopes)) {
                result = Sets.union(result, referencedScopes);
            }
        }
        if (result.isEmpty()) {
            return TransformConfiguration.DEFAULT.getReferencedScopes(variantScope);
        }
        return result;
    }

    @Override
    public Collection<SecondaryFile> getSecondaryFiles() {
        Collection<SecondaryFile> result = new ArrayList<>(super.getSecondaryFiles());
        for (TransformConfiguration config : getConfigurations()) {
            Collection<SecondaryFile> secondaryFiles = config.getSecondaryFiles();
            for (SecondaryFile file : secondaryFiles) {
                if (file != null && !result.contains(file)) {
                    result.add(file);
                }
            }
        }
        return ImmutableList.copyOf(result);
    }

    @Override
    public Collection<File> getSecondaryFileOutputs() {
        Collection<File> result = new ArrayList<>(super.getSecondaryFileOutputs());
        for (TransformConfiguration config : getConfigurations()) {
            Collection<File> secondaryFiles = config.getSecondaryFileOutputs();
            for (File file : secondaryFiles) {
                if (file != null && !result.contains(file)) {
                    result.add(file);
                }
            }
        }
        return ImmutableList.copyOf(result);
    }


    @Override
    public Collection<File> getSecondaryDirectoryOutputs() {
        Collection<File> result = new ArrayList<>(super.getSecondaryDirectoryOutputs());
        for (TransformConfiguration config : getConfigurations()) {
            Collection<File> outputs = config.getSecondaryDirectoryOutputs();
            for (File file : outputs) {
                if (file != null && !result.contains(file)) {
                    result.add(file);
                }
            }
        }
        return ImmutableList.copyOf(result);
    }

    @Override
    public Map<String, Object> getParameterInputs() {
        Map<String, Object> result = new HashMap<>(super.getParameterInputs());
        for (TransformConfiguration config : getConfigurations()) {
            Map<String, Object> parameterInputs = config.getParameterInputs();
            result.putAll(parameterInputs);
        }
        return ImmutableMap.copyOf(result);
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    public boolean shouldSaveCache() {
        return context.extension.isShouldSaveCache() && getPlugins().stream().allMatch(IPlugin::shouldSaveCache);
    }

    @Override
    public final synchronized void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        try {
            GlobalByteXBuildListener.INSTANCE.onByteXPluginTransformStart(this, transformInvocation);
            transformInternal(transformInvocation);
            GlobalByteXBuildListener.INSTANCE.onByteXPluginTransformFinished(this, null);
        } catch (Exception e) {
            GlobalByteXBuildListener.INSTANCE.onByteXPluginTransformFinished(this, e);
            throw e;
        }
    }

    private void transformInternal(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        if (!transformInvocation.isIncremental() && transformInvocation.getOutputProvider() != null) {
            transformInvocation.getOutputProvider().deleteAll();
        }
        TransformContext transformContext = getTransformContext(transformInvocation);
        init(transformContext);
        List<IPlugin> plugins = getPlugins().stream().filter(p -> p.enable(transformContext)).collect(Collectors.toList());
        if (plugins.stream().anyMatch(iPlugin -> !iPlugin.transformConfiguration().isIncremental())) {
            transformContext.requestNotIncremental();
        }

        Timer timer = new Timer();
        final ITransformPipeline manager = new TransformFlowerManager(transformContext);
        try {
            if (!plugins.isEmpty()) {
                plugins.forEach(iPlugin -> iPlugin.startExecute(transformContext));
                plugins.forEach(plugin -> manager.bind(manager1 -> plugin.registerTransformFlow(manager1.getCommonFlow(), transformContext)));
                manager.onPreTransform();
                manager.runTransform();
                manager.onPostTransform();
            } else {
                manager.skipTransform();
            }
            afterTransform(transformInvocation);
        } catch (Throwable throwable) {
            LevelLog.sDefaultLogger.e(throwable.getClass().getName(), throwable);
            throw throwable;
        } finally {
            for (IPlugin plugin : plugins) {
                try {
                    plugin.afterExecute();
                } catch (Throwable throwable) {
                    LevelLog.sDefaultLogger.e("do afterExecute", throwable);
                }
            }
            transformContext.release();
            this.configurations = null;
            timer.record("Total cost time = [%s ms]");
            if (BooleanProperty.ENABLE_HTML_LOG.value()) {
                HtmlReporter.getInstance().createHtmlReporter(getName());
                HtmlReporter.getInstance().reset();
            }
            LevelLog.sDefaultLogger = new LevelLog();
        }
    }

    protected TransformContext getTransformContext(TransformInvocation transformInvocation) {
        return new TransformContext(transformInvocation,
                context.project,
                context.android,
                new TransformOptions.Builder()
                        .setPluginIncremental(isIncremental())
                        .setShouldSaveCache(shouldSaveCache())
                        .setUseRawCache(BooleanProperty.ENABLE_RAM_CACHE.value())
                        .setUseFixedTimestamp(BooleanProperty.USE_FIXED_TIMESTAMP.value())
                        .setForbidUseLenientMutationDuringGetArtifact(BooleanProperty.FORBID_USE_LENIENT_MUTATION_DURING_GET_ARTIFACT.value())
                        .build()
        );
    }

    protected void afterTransform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
    }

    protected void init(TransformContext transformContext) {
        LevelLog.sDefaultLogger = context.getLogger();
        if (BooleanProperty.ENABLE_HTML_LOG.value()) {
            String applicationId = "unknow";
            String versionName = "unknow";
            String versionCode = "unknow";
            com.android.builder.model.ProductFlavor flavor = TransformInvocationKt.getVariant(transformContext.getInvocation()).getMergedFlavor();
            if (flavor != null) {
                String flavorApplicationId = flavor.getApplicationId();
                if (flavorApplicationId != null && !flavorApplicationId.isEmpty()) {
                    applicationId = flavorApplicationId;
                }
                String flavorVersionName = flavor.getVersionName();
                if (flavorVersionName != null && !flavorVersionName.isEmpty()) {
                    versionName = flavorVersionName;
                }
                Integer flavorVersionCode = flavor.getVersionCode();
                if (flavorVersionCode != null) {
                    versionCode = String.valueOf(flavorVersionCode);
                }
            }
            HtmlReporter.getInstance().init(
                    transformContext.byteXBuildDir().getAbsolutePath(),
                    "ByteX",
                    applicationId,
                    versionName,
                    versionCode
            );
        }
    }

    protected abstract List<IPlugin> getPlugins();

    private Set<TransformConfiguration> getConfigurations() {
        if (configurations == null) {
            this.configurations = getPlugins().stream().map(IPlugin::transformConfiguration).collect(Collectors.toSet());
        }
        return this.configurations;
    }
}
