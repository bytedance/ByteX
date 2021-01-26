package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.MethodMatcher;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.refercheck.InaccessibleNode;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * Created by tanlehua on 2018/2/6.
 * 1. 检查每一次方法调用是否存在对应的方法和相应的类
 */

public class ReferCheckClassVisitor extends BaseClassVisitor {
    private final boolean checkInaccessOverrideMethodStrictly;
    private final ReferCheckMethodVisitor.CheckIssueReceiver checkIssueReceiver;
    private final List<MethodMatcher> blockMethodListMatchers;
    private final Graph graph;
    private String className;
    private ClassNode superNode;
    //    private boolean shouldCheck;
    private String sourceFile;

    public ReferCheckClassVisitor(boolean checkInaccessOverrideMethodStrictly, ReferCheckMethodVisitor.CheckIssueReceiver checkIssueReceiver, Graph graph, List<MethodMatcher> blockMethodListMatchers) {
        this.checkInaccessOverrideMethodStrictly = checkInaccessOverrideMethodStrictly;
        this.checkIssueReceiver = checkIssueReceiver;
        this.graph = graph;
        this.blockMethodListMatchers = blockMethodListMatchers;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        if (superName != null) {
            Node superNode = graph.get(superName);
            if (superNode == null) {
                checkIssueReceiver.addNotAccessMember(
                        className, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sourceFile, -1,
                        superName, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, InaccessibleNode.TYPE_CLASS_NOT_FOUND);
            } else if (superNode instanceof InterfaceNode) {
                checkIssueReceiver.addNotAccessMember(
                        className, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sourceFile, -1,
                        superName, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, InaccessibleNode.TYPE_CLASS2INTERFACE);
            } else if (TypeUtil.isFinal(superNode.entity.access)) {
                checkIssueReceiver.addNotAccessMember(
                        className, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sourceFile, -1,
                        superName, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, InaccessibleNode.TYPE_OVERRIDE_FINAL);
            } else {
                this.superNode = (ClassNode) superNode;
            }
        }
        if (interfaces != null) {
            for (String itf : interfaces) {
                Node superItf = graph.get(itf);
                if (superItf == null) {
                    checkIssueReceiver.addNotAccessMember(
                            className, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sourceFile, -1,
                            itf, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, InaccessibleNode.TYPE_CLASS_NOT_FOUND);
                } else if (superItf instanceof ClassNode) {
                    checkIssueReceiver.addNotAccessMember(
                            className, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sourceFile, -1,
                            itf, "<clinit>", "()V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, InaccessibleNode.TYPE_INTERFACE2CLASS);
                }
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        if (!methodName.equals("<init>") && !methodName.equals("<clinit>") && !TypeUtil.isBridge(access)) {
            checkOverride(superNode, access, methodName, desc);
        }
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (!TypeUtil.isNative(access)) {
            mv = new ReferCheckMethodVisitor(mv, checkIssueReceiver, graph, blockMethodListMatchers, className, methodName, desc, access, sourceFile);
        }
        return mv;
    }

    private void checkOverride(ClassNode node, int access, String methodName, String desc) {
        if (node == null) {
            return;
        }
        for (MethodEntity method : node.entity.methods) {
            if (methodName.equals(method.name()) && desc.equals(method.desc())) {
                boolean overrideSuccess = !TypeUtil.isStatic(method.access()) && !TypeUtil.isStatic(access);
                if (TypeUtil.isPublic(method.access())) {
                    if ((!TypeUtil.isStatic(method.access()) && !TypeUtil.isPublic(access)) || (TypeUtil.isStatic(method.access()) != TypeUtil.isStatic(access))) {
                        checkIssueReceiver.addNotAccessMember(
                                className, methodName, desc, access, sourceFile, -1,
                                node.entity.name, methodName, desc, method.access(), InaccessibleNode.TYPE_OVERRIDE_INACCESS);
                        overrideSuccess = false;
                    }
                } else if (TypeUtil.isProtected(method.access())) {
                    if ((!TypeUtil.isStatic(method.access()) && !TypeUtil.isPublic(access) && !TypeUtil.isProtected(access)) || (TypeUtil.isStatic(method.access()) != TypeUtil.isStatic(access))) {
                        checkIssueReceiver.addNotAccessMember(
                                className, methodName, desc, access, sourceFile, -1,
                                node.entity.name, methodName, desc, method.access(), InaccessibleNode.TYPE_OVERRIDE_INACCESS);
                        overrideSuccess = false;
                    }
                } else if (TypeUtil.isPrivate(method.access())) {
                    //不构成复写
                    overrideSuccess = false;
                } else {
                    //default
                    if (Utils.getPackage(className).equals(Utils.getPackage(node.entity.name))) {
                        //构成复写
                        if ((!TypeUtil.isStatic(method.access()) && TypeUtil.isPrivate(access)) || (TypeUtil.isStatic(method.access()) != TypeUtil.isStatic(access))) {
                            checkIssueReceiver.addNotAccessMember(
                                    className, methodName, desc, access, sourceFile, -1,
                                    node.entity.name, methodName, desc, method.access(), InaccessibleNode.TYPE_OVERRIDE_INACCESS);
                            overrideSuccess = false;
                        }
                    }
                }
                if (checkInaccessOverrideMethodStrictly) {
                    if (TypeUtil.isPrivate(access) || TypeUtil.isStatic(access) ||
                            TypeUtil.isPrivate(method.access()) || TypeUtil.isStatic(method.access()) ||
                            (TypeUtil.isPublic(method.access()) && !TypeUtil.isPublic(access)) ||
                            (!TypeUtil.isPublic(method.access()) && !TypeUtil.isProtected(method.access()) && !Utils.getPackage(className).equals(Utils.getPackage(node.entity.name)))) {
                        checkIssueReceiver.addNotAccessMember(
                                className, methodName, desc, access, sourceFile, -1,
                                node.entity.name, methodName, desc, method.access(), InaccessibleNode.TYPE_OVERRIDE_INACCESS_STRICT);

                    }
                }
                if (overrideSuccess) {
                    if (!TypeUtil.isStatic(method.access()) && TypeUtil.isFinal(method.access())) {
                        checkIssueReceiver.addNotAccessMember(
                                className, methodName, desc, access, sourceFile, -1,
                                node.entity.name, methodName, desc, method.access(), InaccessibleNode.TYPE_OVERRIDE_FINAL);
                    }
                    return;
                }
            }
        }
        checkOverride(node.parent, access, methodName, desc);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.sourceFile = source;
    }
}
