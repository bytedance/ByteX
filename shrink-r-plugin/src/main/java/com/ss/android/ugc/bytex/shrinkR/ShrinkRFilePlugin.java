package com.ss.android.ugc.bytex.shrinkR;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.shrinkR.exception.NotFoundRField;
import com.ss.android.ugc.bytex.shrinkR.res_check.AssetsCheckExtension;
import com.ss.android.ugc.bytex.shrinkR.res_check.Resource;
import com.ss.android.ugc.bytex.shrinkR.res_check.ResourceCheckExtension;
import com.ss.android.ugc.bytex.shrinkR.res_check.Substance;
import com.ss.android.ugc.bytex.shrinkR.visitor.AnalyzeRClassVisitor;
import com.ss.android.ugc.bytex.shrinkR.visitor.ShrinkRClassVisitor;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ShrinkRFilePlugin extends CommonPlugin<ShrinkRExtension, Context> {

    @Override
    protected Transform getTransform() {
        return new ShrinkRFileTransform(context, this);
    }

    @Override
    protected Context getContext(Project project, AppExtension android, ShrinkRExtension extension) {
        return new Context(project, android, extension);
    }

    @Override
    public void traverse(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        super.traverse(relativePath, chain);
        if (Utils.isRFile(relativePath)) {
            chain.connect(new AnalyzeRClassVisitor(context));
        }
    }

    @Override
    public void beforeTransform(@NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        context.calculateDiscardableRClasses();
        context.resolveResource();
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        if (context.discardable(relativePath)) {
            context.getLogger().d("DeleteRFile", "Delete R file: " + relativePath);
            return false;
        }
        chain.connect(new ShrinkRClassVisitor(context));
        return super.transform(relativePath, chain);
    }

    @Override
    public void afterTransform(@NotNull TransformEngine engine) {
        super.afterTransform(engine);
        Set<NotFoundRField> notFoundRFields = context.getNotFoundRFields();
        if (!notFoundRFields.isEmpty()) {
            StringBuilder sb = new StringBuilder("This R Class don't exist those field: \n");
            notFoundRFields.forEach(s -> sb.append(String.format("Class [%s] method [%s] reference [%s.%s]\n", s.className, s.methodName,
                    Utils.replaceSlash2Dot(s.owner).replace("$", "."), s.name)));
            RuntimeException exception = new RuntimeException(sb.toString());
            context.getLogger().e("Those R Classes don't exist those field: ", exception);
            if (extension.isStrictCheckMode()) {
                throw exception;
            }
        }
        List<Substance> allUnReachRes = context.getChecker().getAllUnReachResource();
        //扫描到无用string等避免block编译，目前这些都是放在白名单内的
        Set<String> filter = new HashSet<>();
        filter.add("string");
        filter.add("plurals");
        filter.add("array");
        List<Substance> filteredUnReachRes = allUnReachRes.stream().filter(substance -> {
            if (substance instanceof Resource) {
                return !filter.contains(((Resource) substance).getType());
            }
            return true;
        }).collect(Collectors.toList());
        //保存到本地 unused_res.json
        context.getChecker().saveToLocal(allUnReachRes);

        if (!filteredUnReachRes.isEmpty()) {
            RuntimeException exception = new RuntimeException("There are some unused resource, please review those resource and try to delete them, you can check it out in file shrinkR/unused_res.json. \n " +
                    "在你的commit上发现了一些无用的资源，辛苦你review一下这些资源，如果发现确实没有引用，可以尝试删掉，再rebuild。" +
                    "如果有问题请@谭乐华。\n" + context.getChecker().getUnusedAttr(filteredUnReachRes));
            context.getLogger().e("在你的commit上发现了一些无用的资源", exception);
            throw exception;
        }
    }

    @Override
    protected void onApply(@NotNull Project project) {
        super.onApply(project);
        context.resCheckExtension = project.getExtensions().create("resCheck", ResourceCheckExtension.class);
        context.assetsCheckExtension = project.getExtensions().create("assetsCheck", AssetsCheckExtension.class);
    }
}
