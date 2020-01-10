package com.ss.android.ugc.bytex.field_assign_opt.visitors;

import com.ss.android.ugc.bytex.common.utils.OpcodesUtils;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.field_assign_opt.Context;
import com.ss.android.ugc.bytex.field_assign_opt.PutFieldInstructionException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FieldAssignCollectClassVisitor extends BaseClassVisitor {
    private static final String STATIC_CODE_BLOCK_METHOD_NAME = "<clinit>";
    private static final String OBJECT_INIT_CODE_BLOCK_METHOD_NAME = "<init>";
    private static final String DIVIDER_CHAR = " ";
    private final Context mContext;
    private String mClassName;

    public FieldAssignCollectClassVisitor(Context context) {
        this.mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (OBJECT_INIT_CODE_BLOCK_METHOD_NAME.equals(name)) {
            //constructors
            return new RedundantFieldAssignmentMethodVisitor(new MethodNode(access, name, desc, signature, exceptions), mv, false);
        } else if (STATIC_CODE_BLOCK_METHOD_NAME.equals(name) && TypeUtil.isStatic(access)) {
            //static blocks code or static fields assign values block code
            return new RedundantFieldAssignmentMethodVisitor(new MethodNode(access, name, desc, signature, exceptions), mv, true);
        }
        return mv;
    }

    class RedundantFieldAssignmentMethodVisitor extends MethodVisitor {

        final boolean isStatic;
        MethodNode mn;
        MethodVisitor mv;
        final Set<Integer> mBreakInstructions;

        RedundantFieldAssignmentMethodVisitor(MethodNode mn, MethodVisitor mv, boolean isStatic) {
            super(Opcodes.ASM5, mn);
            this.mn = mn;
            this.mv = mv;
            this.isStatic = isStatic;
            mBreakInstructions = new HashSet<>();
            for (int i = Opcodes.IFEQ; i <= Opcodes.RETURN; i++) {
                mBreakInstructions.add(i);
            }
            mBreakInstructions.add(Opcodes.IFNULL);
            mBreakInstructions.add(Opcodes.IFNONNULL);
            mBreakInstructions.add(200); // int GOTO_W = 200; // -
            mBreakInstructions.add(201); // int JSR_W = 201; // -
            mBreakInstructions.add(Opcodes.INVOKEVIRTUAL);
            mBreakInstructions.add(Opcodes.INVOKESPECIAL);
            mBreakInstructions.add(Opcodes.INVOKESTATIC);
            mBreakInstructions.add(Opcodes.INVOKEINTERFACE);
            mBreakInstructions.add(Opcodes.INVOKEDYNAMIC);
            mBreakInstructions.add(Opcodes.NEW);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final int size = mn.instructions.size();
            final List<List<AbstractInsnNode>> redundantInsNodes = new LinkedList<>();
            final Set<String> visitedPutFields = new HashSet<>();
            boolean calledSuper = false;
            for (int i = 0; i < size; i++) {
                AbstractInsnNode abstractInsnNode = mn.instructions.get(i);
                if (abstractInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && !calledSuper) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (isStatic) {
                        //static block code does not have super call
                        break;
                    } else if (OBJECT_INIT_CODE_BLOCK_METHOD_NAME.equals(methodInsnNode.name)) {
                        if (methodInsnNode.owner.equals(mClassName)) {
                            //call this constructor,ignore all subsequent instructions
                            break;
                        }
                        //call super constructor
                        calledSuper = true;
                    } else {
                        /*
                         * for example
                         * public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
                         *    this(owner.getViewModelStore(), factory);
                         * }
                         *
                         * It is safe to continue because all members in this object are invisible to others before finish super constructor call
                         *
                         * */
                        mContext.getLogger().d(mClassName + ":Super constructor call is not the first MethodInsnNode???-" + methodInsnNode.name);
                        System.out.println("Super constructor call is not the first MethodInsnNode???-" + methodInsnNode.name);
                    }
                } else if (mBreakInstructions.contains(abstractInsnNode.getOpcode())) {
                    //jump instructions,ignore all subsequent instructions
                    break;
                } else if (abstractInsnNode instanceof FieldInsnNode) {
                    final FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    if (fieldInsnNode.getOpcode() == (isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD)) {
                        if (mContext.inWhiteList(fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc)) {
                            //whitelist ignore
                            continue;
                        }
                        if (!fieldInsnNode.owner.equals(mClassName)) {
                            continue;
                        }
                        final String uniqueKey = getUniqueKey(fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
                        if (visitedPutFields.contains(uniqueKey)) {
                            //already visited PUTSTATIC instruction or PUTFIELD instruction on this field
                            continue;
                        }
                        /*
                            assign a static field code will be like this:
                                mv.visitLabel(l0);
                                mv.visitLineNumber(40, l0);
                                mv.visitInsn(ICONST_0);
                                mv.visitFieldInsn(PUTSTATIC, "com/ss/android/ugc/aweme/yangzhiqian/demoapp/asm/A", "j", "I");

                             assign a object field code will be like this:
                                mv.visitLabel(l5);
                                mv.visitLineNumber(34, l5);
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitInsn(ICONST_0);
                                mv.visitFieldInsn(PUTFIELD, "com/ss/android/ugc/aweme/yangzhiqian/demoapp/asm/A", "e", "I");
                        */
                        final int visitVarInsnIndex = i - 2;
                        final int visitInsnIndex = i - 1;
                        if (isStatic ? visitInsnIndex < 0 : visitVarInsnIndex < 0) {
                            throw new PutFieldInstructionException("");
                        }

                        //check visitInsn
                        AbstractInsnNode insnNode = mn.instructions.get(visitInsnIndex);
                        if (!isPutFieldWithDefaultAssignment(insnNode, fieldInsnNode.desc)) {
                            visitedPutFields.add(uniqueKey);//store visit PUTSTATIC instruction or PUTFIELD instruction state
                            continue;
                        }
                        //check visitVarInsn
                        AbstractInsnNode varInsnNode = null;
                        if (!isStatic) {
                            varInsnNode = mn.instructions.get(visitVarInsnIndex);
                            if (varInsnNode.getOpcode() != Opcodes.ALOAD || !(varInsnNode instanceof VarInsnNode) || ((VarInsnNode) varInsnNode).var != 0) {
                                visitedPutFields.add(uniqueKey);//store visit PUTSTATIC instruction or PUTFIELD instruction state
                                continue;
                            }
                        }
                        //redundant,add InsnNode
                        List<AbstractInsnNode> nodeList = new ArrayList<>();
                        if (!isStatic) {
                            nodeList.add(varInsnNode);
                        }
                        nodeList.add(insnNode);
                        nodeList.add(fieldInsnNode);
                        redundantInsNodes.add(nodeList);

                        if (!mContext.extension.isRemoveLineNumber()) {
                            continue;
                        }
                        //deal with line number and label
                        int visitLineNumberIndex = i - (isStatic ? 2 : 3);//mv.visitLineNumber(47, l1);
                        if (visitLineNumberIndex < 0) {
                            continue;
                        }
                        AbstractInsnNode node = mn.instructions.get(visitLineNumberIndex);
                        if (node instanceof LineNumberNode) {
                            nodeList.add(0, node);
                        }
                    }
                }
            }
            //delete redundant instruction Node
            for (List<AbstractInsnNode> list : redundantInsNodes) {
                StringBuilder stringBuilder = new StringBuilder()
                        .append("remove instructions:").append(list.size()).append("\t")
                        .append("class:").append(mClassName).append("\t")
                        .append(isStatic ? "clinit" : "init")
                        .append("\n");
                for (AbstractInsnNode node : list) {
                    stringBuilder.append(OpcodesUtils.covertToString(node));
                    stringBuilder.append("\n");
                    mn.instructions.remove(node);
                }
                mContext.getLogger().i(mClassName, stringBuilder.toString());
            }
            //write to super
            mn.accept(mv);
        }


        private boolean isPutFieldWithDefaultAssignment(AbstractInsnNode node, String fieldDesc) {
            switch (fieldDesc) {
                case "Z":
                case "B":
                case "C":
                case "S":
                case "I":
                    //boolean、byte、char、short、int
                    return node.getOpcode() == Opcodes.ICONST_0;
                case "F":
                    return node.getOpcode() == Opcodes.FCONST_0;
                case "D":
                    return node.getOpcode() == Opcodes.DCONST_0;
                case "J":
                    return node.getOpcode() == Opcodes.LCONST_0;
                default: //object void array
                    return node.getOpcode() == Opcodes.ACONST_NULL;
            }
        }
    }


    private static String getUniqueKey(String owner, String name, String desc) {
        //"-" is a illegal character
        return owner + "-" + name + "-" + desc;
    }
}
