package com.ss.android.ugc.bytex.coverage_plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.internal.pipeline.TransformTask;
import com.google.common.base.Joiner;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.TransformConfiguration;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.coverage_plugin.util.GraphUtil;
import com.ss.android.ugc.bytex.coverage_plugin.util.MappingIdGen;
import com.ss.android.ugc.bytex.coverage_plugin.util.ProguardKt;
import com.ss.android.ugc.bytex.coverage_plugin.visitors.CoverageClassVisitor;
import com.ss.android.ugc.bytex.gradletoolkit.TransformInvocationKt;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.annotation.Nonnull;

import static com.android.builder.model.AndroidProject.FD_OUTPUTS;

/**
 * Created by jiangzilai on 2019-07-15.
 */
public class CoveragePlugin extends CommonPlugin<CoverageExtension, Context> {

    private MappingIdGen mappingIdGen;

    @Override
    protected Context getContext(Project project, AppExtension android, CoverageExtension extension) {
        Context context = new Context(project, android, extension);
        context.setClInitOnly(extension.isClInitOnly());
        return context;
    }

    @Override
    public boolean transform(@NotNull @Nonnull String relativePath, @NotNull @Nonnull ClassVisitorChain chain) {
        if (context.getWhiteList().shouldCheck(relativePath)) {
            chain.connect(new CoverageClassVisitor(context, mappingIdGen));
        }
        return true;
    }


    @Override
    public void init() {
        super.init();
        // 由于是在Proguard之后插桩，尝试拉取合适的mapping反混淆
        // try to get the mapping file to parse the raw className owing to we do this after Proguard
        BaseVariant variant = context.getTransformContext().getVariant();
        File mappingFile = new File(Joiner.on(File.separatorChar).join(
                String.valueOf(project.getBuildDir()),
                FD_OUTPUTS,
                "mapping",
                variant.getFlavorName(),
                variant.getBuildType().getName(),
                "mapping.txt"));
        if (mappingFile.exists()) {
            context.setProguardMap(ProguardKt.initProguardMapping(mappingFile));
        } else {
            context.setProguardMap(new HashMap<>(0));
        }

        String variantName = variant.getFlavorName() + variant.getBuildType().getName().substring(0, 1).toUpperCase() +
                variant.getBuildType().getName().substring(1);
        context.getLogger().d("versionName:" + variantName);
        context.setVersionName(variantName);
        context.setBasePath(context.buildDir().getPath() + File.separator + Context.getModuleName());
        String mappingPath = context.getBasePath() + File.separator + "mapping_"
                + context.getVersionName() + "_" + new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA).format(new Date()) + ".txt";
        context.setMappingFilePath(mappingPath);
        context.setMappingLatestFilePath(context.getBasePath() + File.separator + "mapping_latest.txt");
        context.setGraphFilePath(mappingPath.replace("mapping", "graph"));
        try {
            mappingIdGen = new MappingIdGen(context);
        } catch (IOException e) {
            context.getLogger().e("mapping初始化失败, failed to init mapping");
            e.printStackTrace();
        }
    }


    @Override
    public void afterTransform(@NotNull @Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        try {
            mappingIdGen.saveMapping();
        } catch (IOException e) {
            e.printStackTrace();
            context.getLogger().e("保存mapping失败, failed to save mapping");
            System.exit(0);
        }
        try {
            // 保存类图到文件
            // serialize the graph to file
            GraphUtil.saveGraph(context);
        } catch (Exception e) {
            e.printStackTrace();
            context.getLogger().e("保存graph失败, failed to save graph");
            System.exit(0);
        }
        mappingIdGen.clean();
        context.setProguardMap(null);
    }

    @Override
    public boolean hookTask() {
        return true;
    }

    @Nonnull
    @Override
    public HookType hookTask(@Nonnull Task task) {
        if (task instanceof TransformTask) {
            if (((TransformTask) task).getTransform().getName().equals("proguard")) {
                return HookType.After;
            }
        }
        return HookType.None;
    }

    @Override
    public void afterExecute() throws Throwable {
        super.afterExecute();
        mappingIdGen = null;
    }

    @Nonnull
    @Override
    public TransformConfiguration transformConfiguration() {
        return new TransformConfiguration() {
            @Override
            public boolean isIncremental() {
                return false;
            }
        };
    }
}
