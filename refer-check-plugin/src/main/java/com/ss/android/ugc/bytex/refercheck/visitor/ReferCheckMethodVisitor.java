package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.graph.ClassEntity;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
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
    private String methodName;
    private String className;
    private int processingLineNumber;

    ReferCheckMethodVisitor(MethodVisitor mv, ReferCheckContext context, String methodName, String className, String sourceFile) {
        super(Opcodes.ASM5, mv);
        this.context = context;
        this.methodName = methodName;
        this.className = className;
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

    private void checkMethod(int opcode, String owner, String name, String desc, boolean itf) {
        if (context.shouldCheck(owner, name)) {
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
                            graph.traverseChildren((InterfaceNode) ownerNode, node -> {
                                ClassEntity clz = node.entity;
                                if (!TypeUtil.isInterface(clz.access) && !TypeUtil.isAbstract(clz.access)
                                        && clz.methods.stream().noneMatch(m -> !TypeUtil.isAbstract(m.access()) && name.equals(m.name()) && desc.equals(m.desc()))) {
                                    context.addMethodIfNeed(-1, clz.name, name, desc, className, methodName, processingLineNumber, sourceFile);
                                }
                                return false;
                            });
                        } else { // from abstract class
                            if (!(ownerNode instanceof ClassNode)) {
                                throw new RuntimeException(String.format("%s should be a class, but it's an interface now. It was referred at class [%s], method [%s].", ownerNode.entity.name, this.className, this.methodName));
                            }
                            graph.traverseChildren((ClassNode) ownerNode, node -> {
                                ClassEntity clz = node.entity;
                                if (!TypeUtil.isInterface(clz.access) && !TypeUtil.isAbstract(clz.access)
                                        && clz.methods.stream().noneMatch(m -> !TypeUtil.isAbstract(m.access()) && name.equals(m.name()) && desc.equals(m.desc()))) {
                                    context.addMethodIfNeed(-1, clz.name, name, desc, className, methodName, processingLineNumber, sourceFile);
                                }
                                return false;
                            });
                        }
                    }
                    if (accessible(opcode, originMethod)) {
                        return;
                    }
                }
            }
            context.addNotAccessMethod(owner, name, desc, className, methodName, processingLineNumber, sourceFile);
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
        if (context.shouldCheck(owner, name)) {
            Node ownerNode = graph.get(owner);
            if (ownerNode != null) {
                FieldEntity originField = ownerNode.confirmOriginField(name, desc);
                if (originField != null) {
                    if (!accessible(opcode, originField)) {
                        context.addFieldIfNeed(originField.access(), owner, name, desc, className, methodName, processingLineNumber, sourceFile);
                    }
                    return; // this field exist.
                }
            }
            context.addFieldIfNeed(-1, owner, name, desc, className, methodName, processingLineNumber, sourceFile);
        }
    }
}
