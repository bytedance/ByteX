package com.ss.android.ugc.bytex.const_inline;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.CommonPlugin;
import com.ss.android.ugc.bytex.common.TransformConfiguration;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.const_inline.reflect.ReflectResolve;
import com.ss.android.ugc.bytex.const_inline.reflect.model.ReflectFieldModel;
import com.ss.android.ugc.bytex.const_inline.visitors.InlineConstClassVisitor;
import com.ss.android.ugc.bytex.const_inline.visitors.InlineConstPreviewClassVisitor;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;

@PluginConfig("bytex.const_inline")
public class ConstInlinePlugin extends CommonPlugin<ConstInlineExtension, Context> {
    @Override
    protected Context getContext(Project project, AppExtension android, ConstInlineExtension extension) {
        return new Context(project, android, extension);
    }

    @Override
    public void traverse(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        //do not scan R.class
        if (!Utils.isRFile(relativePath)) {
            chain.connect(new InlineConstPreviewClassVisitor(context));
        }
        super.traverse(relativePath, chain);
    }

    @Override
    public void traverse(@Nonnull String relativePath, @Nonnull ClassNode node) {
        super.traverse(relativePath, node);
        if (context.extension.isAutoFilterReflectionField()) {
            //analyze reflection
            for (Object method : node.methods) {
                MethodNode methodNode = (MethodNode) method;
                int size = methodNode.instructions.size();
                for (int index = 0; index < size; index++) {
                    AbstractInsnNode insnNode = methodNode.instructions.get(index);
                    if (insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if ("java/lang/Class".equals(methodInsnNode.owner)) {
                            if ("getField".equals(methodInsnNode.name)) {
                                String fieldName = ReflectResolve.resolveLastLdc(index, methodNode, String.class);
                                if (fieldName != null) {
                                    String className = ReflectResolve.resolveLastLoadClass(index - 1, methodNode);
                                    context.addReflectClassConstField(new ReflectFieldModel(className, fieldName, false));
                                }
                            } else if ("getFields".equals(methodInsnNode.name)) {
                                String className = ReflectResolve.resolveLastLoadClass(index, methodNode);
                                if (className != null) {
                                    context.addReflectClassConstField(new ReflectFieldModel(className, null, false));
                                }
                            } else if ("getDeclaredField".equals(methodInsnNode.name)) {
                                String fieldName = ReflectResolve.resolveLastLdc(index, methodNode, String.class);
                                if (fieldName != null) {
                                    String className = ReflectResolve.resolveLastLoadClass(index - 1, methodNode);
                                    context.addReflectClassConstField(new ReflectFieldModel(className, fieldName, true));
                                }
                            } else if ("getDeclaredFields".equals(methodInsnNode.name)) {
                                String className = ReflectResolve.resolveLastLoadClass(index, methodNode);
                                if (className != null) {
                                    context.addReflectClassConstField(new ReflectFieldModel(className, null, true));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void beforeTransform(@NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        context.prepare();
    }

    @Override
    public boolean transform(@NotNull String relativePath, @NotNull ClassVisitorChain chain) {
        if (!Utils.isRFile(relativePath)) {
            chain.connect(new InlineConstClassVisitor(context));
        }
        return super.transform(relativePath, chain);
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
