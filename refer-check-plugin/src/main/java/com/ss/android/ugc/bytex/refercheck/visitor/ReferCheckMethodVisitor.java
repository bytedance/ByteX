package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.MethodMatcher;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.refercheck.InaccessibleNode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by tlh on 2018/2/13.
 */

public class ReferCheckMethodVisitor extends MethodVisitor {
    private final CheckIssueReceiver checkIssueReceiver;
    private final String sourceFile;
    private final Graph graph;
    private final List<MethodMatcher> blockMethodListMatchers;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final int methodAccess;
    private int processingLineNumber;

    ReferCheckMethodVisitor(MethodVisitor mv, CheckIssueReceiver checkIssueReceiver, Graph graph,
                            List<MethodMatcher> blockMethodListMatchers,
                            String className, String methodName, String methodDesc, int methodAccess, String sourceFile) {
        super(Constants.ASM_API, mv);
        this.checkIssueReceiver = checkIssueReceiver;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodAccess = methodAccess;
        this.sourceFile = sourceFile;
        this.graph = graph;
        this.blockMethodListMatchers = blockMethodListMatchers;
        if (TypeUtil.isAbstract(methodAccess)) {
            boolean itf = graph.get(className) instanceof InterfaceNode;
            checkMethod(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, className, methodName, methodDesc, itf);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        processingLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        for (MethodMatcher blockMethodListMatcher : blockMethodListMatchers) {
            if (blockMethodListMatcher.match(owner, name, desc)) {
                checkIssueReceiver.addNotAccessMember(
                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                        owner, name, desc, 0, InaccessibleNode.TYPE_CALL_BLOCK_METHOD);
                break;
            }
        }
        checkMethod(opcode, owner, name, desc, itf);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void checkMethod(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
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
                            if (child instanceof ClassNode && !TypeUtil.isAbstract(child.entity.access)) {
                                MethodEntity childImpl = child.confirmOriginMethod(name, desc);
                                if (childImpl == null || TypeUtil.isAbstract(childImpl.access())) {
                                    //非抽象子类没有实现这个抽象类
                                    checkIssueReceiver.addNotAccessMember(
                                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                                            child.entity.name, name, desc, originMethod.access(), InaccessibleNode.TYPE_NOT_IMPLEMENT);
                                }
                            }
                            return false;
                        });
                    } else { // from abstract class
                        if (!(ownerNode instanceof ClassNode)) {
                            throw new RuntimeException(String.format("%s should be a class, but it's an interface now. It was referred at class [%s], method [%s].", ownerNode.entity.name, this.className, this.methodName));
                        }
                        graph.traverseChildren((ClassNode) ownerNode, child -> {
                            if (!TypeUtil.isAbstract(child.entity.access)) {
                                MethodEntity childImpl = child.confirmOriginMethod(name, desc);
                                if (childImpl == null || TypeUtil.isAbstract(childImpl.access())) {
                                    //非抽象子类没有实现这个抽象方法
                                    checkIssueReceiver.addNotAccessMember(
                                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                                            child.entity.name, name, desc, originMethod.access(), InaccessibleNode.TYPE_NOT_IMPLEMENT);
                                }
                            }
                            return false;
                        });
                    }
                }
                if (!accessible(opcode, originMethod)) {
                    checkIssueReceiver.addNotAccessMember(
                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                            owner, name, desc, originMethod.access(), InaccessibleNode.TYPE_INACCESS);
                }
            } else {
                checkIssueReceiver.addNotAccessMember(
                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                        owner, name, desc, 0, InaccessibleNode.TYPE_METHOD_NOT_FOUND);
            }
        } else {
            checkIssueReceiver.addNotAccessMember(
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
                    checkIssueReceiver.addNotAccessMember(
                            className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                            owner, name, desc, originField.access(), InaccessibleNode.TYPE_INACCESS);
                }
            } else {
                checkIssueReceiver.addNotAccessMember(
                        className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                        owner, name, desc, 0, InaccessibleNode.TYPE_FIELD_NOT_FOUND);
            }
        } else {
            checkIssueReceiver.addNotAccessMember(
                    className, methodName, methodDesc, methodAccess, sourceFile, processingLineNumber,
                    owner, name, desc, 01, InaccessibleNode.TYPE_CLASS_NOT_FOUND);
        }
    }

    public interface CheckIssueReceiver {
        void addNotAccessMember(String callClassName, String callMethodName, String callMethodDesc, int callMethodAccess, @Nullable String sourceFile, int line,
                                String memberOwner, String memberName, String memberDesc, int memberAccess,
                                int type);

        List<InaccessibleNode> getInaccessibleNodes();
    }
}
