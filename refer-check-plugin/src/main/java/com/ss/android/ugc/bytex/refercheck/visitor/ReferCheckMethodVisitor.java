package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.refercheck.InaccessibleNode;
import com.ss.android.ugc.bytex.refercheck.ReferCheckContext;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * Created by tlh on 2018/2/13.
 */

public class ReferCheckMethodVisitor extends MethodVisitor {
    private final ReferCheckContext context;
    private final String sourceFile;
    private final Graph graph;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final int methodAccess;
    private int processingLineNumber;

    ReferCheckMethodVisitor(MethodVisitor mv, ReferCheckContext context,
                            String className, String methodName, String methodDesc, int methodAccess, String sourceFile) {
        super(Opcodes.ASM5, mv);
        this.context = context;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodAccess = methodAccess;
        this.sourceFile = sourceFile;
        this.graph = context.getClassGraph();
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        processingLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        checkMethod(opcode, owner, name, desc, itf);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void checkMethod(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
        String method = String.format("%s#%s", owner, name);
        List<String> logMethods = context.extension.getLogMethods();
        if (logMethods != null && logMethods.contains(method)) {
            context.getLogger().i("Check method : " + method);
        }
        Node ownerNode = graph.get(owner);
        if (ownerNode != null) {
            MethodEntity originMethod = ownerNode.confirmOriginMethod(name, desc);
            if (originMethod != null) {
                if (TypeUtil.isAbstract(originMethod.access())) {
                    // Add all children to checklist
                    if (itf) { // from interface
                        if (!(ownerNode instanceof InterfaceNode)) {
                            throw new RuntimeException(String.format("%s should be a interface, but it's a class now. It was referred at class [%s], method [%s].", ownerNode.entity.name, this.className, this.methodName));
                        }
                        graph.traverseChildren((InterfaceNode) ownerNode, child -> {
                            if (child instanceof ClassNode && !TypeUtil.isAbstract(child.entity.access)
                                    && TypeUtil.isAbstract(child.confirmOriginMethod(name, desc).access())) {
                                //非抽象子类没有实现这个抽象类
                                context.addNotAccessMember(
                                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                                        child.entity.name, name, desc, originMethod.access(), InaccessibleNode.TYPE_NOT_IMPLEMENT);
                            }
                            return false;
                        });
                    } else { // from abstract class
                        if (!(ownerNode instanceof ClassNode)) {
                            throw new RuntimeException(String.format("%s should be a class, but it's an interface now. It was referred at class [%s], method [%s].", ownerNode.entity.name, this.className, this.methodName));
                        }
                        graph.traverseChildren((ClassNode) ownerNode, child -> {
                            if (!TypeUtil.isAbstract(child.entity.access) && TypeUtil.isAbstract(child.confirmOriginMethod(name, desc).access())) {
                                //非抽象子类没有实现这个抽象方法
                                context.addNotAccessMember(
                                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                                        child.entity.name, name, desc, originMethod.access(), InaccessibleNode.TYPE_NOT_IMPLEMENT);
                            }
                            return false;
                        });
                    }
                }
                if (!accessible(opcode, originMethod)) {
                    context.addNotAccessMember(
                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                            owner, name, desc, originMethod.access(), InaccessibleNode.TYPE_INACCESS);
                }
            } else {
                context.addNotAccessMember(
                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                        owner, name, desc, 0, InaccessibleNode.TYPE_METHOD_NOT_FOUND);
            }
        } else {
            context.addNotAccessMember(
                    className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                    owner, name, desc, 0, InaccessibleNode.TYPE_CLASS_NOT_FOUND);
        }
    }

    private boolean accessible(int opcode, MemberEntity originMember) {
        boolean isStaticMember = TypeUtil.isStatic(originMember.access());
        if (
                (opcode == Opcodes.INVOKESTATIC) == isStaticMember ||
                        (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) == isStaticMember
        ) {
            return accessible(originMember);
        }
        return false;
    }

    private boolean accessible(MemberEntity member) {
        if (className.equals(member.className())) {
            return true;
        }
        if (TypeUtil.isPublic(member.access())) {
            return true;
        } else if (TypeUtil.isProtected(member.access())) {
            //同包名或者集成关系 At same package or inheritance relationship
            return Utils.getPackage(className).equals(Utils.getPackage(member.className())) ||
                    graph.get(this.className).inheritFrom(graph.get(member.className()));
        } else if (TypeUtil.isPrivate(member.access())) {
            return false;
        } else {
            //package
            return Utils.getPackage(className).equals(Utils.getPackage(member.className()));
        }
    }


    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        checkField(opcode, name, desc, owner);
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    private void checkField(int opcode, String name, String desc, String owner) {
        Node ownerNode = graph.get(owner);
        if (ownerNode != null) {
            FieldEntity originField = ownerNode.confirmOriginField(name, desc);
            if (originField != null) {
                if (!accessible(opcode, originField)) {
                    context.addNotAccessMember(
                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                            owner, name, desc, originField.access(), InaccessibleNode.TYPE_INACCESS);
                }
            } else {
                context.addNotAccessMember(
                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                        owner, name, desc, 0, InaccessibleNode.TYPE_FIELD_NOT_FOUND);
            }
        } else {
            context.addNotAccessMember(
                    className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                    owner, name, desc, 01, InaccessibleNode.TYPE_CLASS_NOT_FOUND);
        }
    }
}
