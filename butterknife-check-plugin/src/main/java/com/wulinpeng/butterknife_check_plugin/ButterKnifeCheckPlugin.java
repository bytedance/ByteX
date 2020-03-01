package com.wulinpeng.butterknife_check_plugin;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.TransformConfiguration;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * author：wulinpeng
 * date：2020-02-18 11:50
 * desc:
 */
//@PluginConfig("bytex.butterknife-check")
public class ButterKnifeCheckPlugin extends CommonPlugin<ButterKnifeCheckExtension, Context> {

    private static final String BUTTERKNIFE_SUFFIX = "_ViewBinding";
    private static final String UNBINDER_INTRFACE = "butterknife/Unbinder";
    private static final String OBJECT_CLASS = "java/lang/Object";

    @Override protected Context getContext(Project project, AppExtension android, ButterKnifeCheckExtension extension) {
        return new Context(project, android, extension);
    }

    @Override public boolean transform(@Nonnull String relativePath, @Nonnull org.objectweb.asm.tree.ClassNode node) {
        return super.transform(relativePath, node);
    }

    @Override public void afterTransform(@Nonnull TransformEngine engine) {
        super.afterTransform(engine);
        Graph graph = context.getClassGraph();
        Map<ClassNode, ClassNode> errorViewBinding = new HashMap<>();
        System.out.println("start ButterKnife check");
        for (ClassNode classNode : context.getClassGraph().implementsOf(UNBINDER_INTRFACE)) {
            // 非ViewBinding或存在继承关系则跳过
            if (!classNode.entity.name.endsWith(BUTTERKNIFE_SUFFIX) || !classNode.parent.entity.name.equals(OBJECT_CLASS)) {
                continue;
            }
            try {
                ClassNode originClass = (ClassNode) graph.get(classNode.entity.name.replace(BUTTERKNIFE_SUFFIX, ""));
                ClassNode parent = originClass.parent;
                while (parent != null) {
                    if (graph.get(parent.entity.name + BUTTERKNIFE_SUFFIX) != null) {
                        errorViewBinding.put(originClass, parent);
                        break;
                    }
                    parent = parent.parent;
                }
            } catch (Throwable t) {
            }
        }
        if (errorViewBinding.size() == 0) {
            System.out.println("ButterKnife check passed");
            return;
        }
        for (ClassNode classNode : errorViewBinding.keySet()) {
            ClassNode superClass = errorViewBinding.get(classNode);
            System.out.println("Class " + classNode.entity.name + " and super class " + superClass.entity.name
                    + " can not in the different modules because of Bug of ButterKnife");
        }
        throw new RuntimeException("");
    }
}
