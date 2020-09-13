package com.ss.android.ugc.bytex.access_inline.visitor;

import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.MemberType;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.access_inline.Context;
import com.ss.android.ugc.bytex.access_inline.ShouldSkipInlineException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class PreProcessClassVisitor extends BaseClassVisitor {

    private Context context;
    private boolean fromAndroidSDK;
    private String className;

    public PreProcessClassVisitor(Context context) {
        this(context, false);
    }

    public PreProcessClassVisitor(Context context, boolean fromAndroidSDK) {
        this.context = context;
        this.fromAndroidSDK = fromAndroidSDK;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!fromAndroidSDK && TypeUtil.isSynthetic(access) && TypeUtil.isStatic(access) && name.startsWith("access$")) {
            Access$MethodEntity access$MethodEntity = new Access$MethodEntity(className, name, desc);
            return new RefineAccess$MethodVisitor(context, access$MethodEntity);
        }
        return mv;
    }

    private static class RefineAccess$MethodVisitor extends MethodNode {
        private Access$MethodEntity access$MethodEntity;
        private Context context;

        public RefineAccess$MethodVisitor(Context context, Access$MethodEntity access$MethodEntity) {
            super(Opcodes.ASM5);
            this.access$MethodEntity = access$MethodEntity;
            this.context = context;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            List<AbstractInsnNode> refinedList = refine(instructions);
            if (!refinedList.isEmpty()) {
                access$MethodEntity.setInsnNodeList(refinedList);
                context.addAccess$Method(access$MethodEntity);
            }
        }

        private List<AbstractInsnNode> refine(InsnList instructions) {
            List<AbstractInsnNode> refinedInsns = new ArrayList<>();
            if (instructions == null) {
              return refinedInsns;
            }
            ListIterator<AbstractInsnNode> insnNodeListIterator = instructions.iterator();
            boolean shouldSkipVarInsn = true;
            int varLoadInsnCount = 0;
            MemberEntity target = null;
            try {
                while (insnNodeListIterator.hasNext()) {
                    AbstractInsnNode insnNode = insnNodeListIterator.next();
                    if (insnNode.getType() == AbstractInsnNode.LINE) continue;
                    if (insnNode.getType() == AbstractInsnNode.LABEL) continue;
                    if (insnNode.getOpcode() >= Opcodes.IRETURN && insnNode.getOpcode() <= Opcodes.RETURN)
                        break;
                    if (insnNode.getType() == AbstractInsnNode.JUMP_INSN) {
                        throw new ShouldSkipInlineException("Unexpected JUMP_INSN instruction in access$ method body.");
                    }
                    if (insnNode.getOpcode() == Opcodes.ATHROW) {
                        throw new ShouldSkipInlineException("Unexpected ATHROW instruction in access$ method body.");
                    }
                    // no control instruction
                    if (insnNode.getOpcode() >= Opcodes.GOTO && insnNode.getOpcode() <= Opcodes.LOOKUPSWITCH) {
                        throw new ShouldSkipInlineException("Unexpected control instruction in access$ method body.");
                    }
                    // If those instructions appear in access$ method body, skip inline it.
                    // 如果在access$方法内部有这些指令，则不内联这个方法，因为这些指令都比较新，for safe.
                    if (insnNode.getOpcode() > Opcodes.MONITOREXIT) {
                        throw new ShouldSkipInlineException("Unexpected new instruction in access$ method body.");
                    }

                    if (shouldSkipVarInsn && insnNode.getOpcode() >= Opcodes.ILOAD && insnNode.getOpcode() <= Opcodes.SALOAD) {
                        varLoadInsnCount++;
                        continue;
                    }
                    // no store instruction
                    if (insnNode.getOpcode() >= Opcodes.ISTORE && insnNode.getOpcode() <= Opcodes.SASTORE) {
                        throw new ShouldSkipInlineException("Unexpected store instruction in access$ method body.");
                    }
                    if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                        if (shouldSkipVarInsn) {
                            shouldSkipVarInsn = false;
                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            target = new MethodEntity(MemberEntity.ACCESS_UNKNOWN, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
                            int parameterCountOfTargetMethod = TypeUtil.getParameterCountFromMethodDesc(methodInsnNode.desc);
                            int parameterCountOfAccess$Method = TypeUtil.getParameterCountFromMethodDesc(access$MethodEntity.desc());
                            switch (insnNode.getOpcode()) {
                                case Opcodes.INVOKEVIRTUAL:
                                case Opcodes.INVOKESPECIAL:
                                case Opcodes.INVOKEINTERFACE:
                                    if (parameterCountOfTargetMethod != parameterCountOfAccess$Method - 1
                                        || varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                case Opcodes.INVOKESTATIC:
                                    if (parameterCountOfTargetMethod != parameterCountOfAccess$Method ||
                                        varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                default:
                                    throw new ShouldSkipInlineException("The instruction of access$ method is unknown.");
                            }
                            access$MethodEntity.setTarget(context.addAccessedMembers(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, false));
                        }
                        refinedInsns.add(insnNode);
                    } else if (insnNode.getType() == AbstractInsnNode.FIELD_INSN) {
                        if (shouldSkipVarInsn) {
                            shouldSkipVarInsn = false;
                            FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                            target = new FieldEntity(MemberEntity.ACCESS_UNKNOWN, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
                            int parameterCountOfAccess$Method = TypeUtil.getParameterCountFromMethodDesc(access$MethodEntity.desc());
                            switch (insnNode.getOpcode()) {
                                case Opcodes.GETSTATIC:
                                    if (parameterCountOfAccess$Method != 0
                                        || varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                case Opcodes.GETFIELD:
                                    if (parameterCountOfAccess$Method != 1
                                            || varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                case Opcodes.PUTFIELD:
                                    if (parameterCountOfAccess$Method != 2 ||
                                            varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                case Opcodes.PUTSTATIC:
                                    if (parameterCountOfAccess$Method != 1 ||
                                            varLoadInsnCount != parameterCountOfAccess$Method) {
                                        throw new ShouldSkipInlineException("The parameter count of access$ method is abnormal.");
                                    }
                                    break;
                                default:
                                    throw new ShouldSkipInlineException("The instruction of access$ method is unknown.");
                            }
                            access$MethodEntity.setTarget(context.addAccessedMembers(fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc, true));
                        }
                        refinedInsns.add(insnNode);
                    } else {
                        refinedInsns.add(insnNode);
                    }
                }
            } catch (ShouldSkipInlineException e) {
                if (target != null) {
                    context.getLogger().d("SkipInlineAccess", String.format("Skip inline access to %s (owner = [%s], name = [%s], desc = [%s]), for the reason that %s",
                            target.type() == MemberType.FIELD ? FieldEntity.class.getSimpleName() : MethodEntity.class.getSimpleName(),
                            target.className(), target.name(), target.desc(), e.reason));
                }
                refinedInsns.clear();
            }

            /**
             * 没有找到target时，导致使用target时候NPE问题
             * 清除需要内联的指令
             *
             * 例如：
             * 遇到只有LDC指令的access$方法， 无法给access$MethodEntity 设置target
             *
             */
            if (target == null) {
                refinedInsns.clear();
                if (access$MethodEntity != null) {
                    context.getLogger().d("SkipInlineAccess", String.format("Skip inline access to %s (owner = [%s], name = [%s], desc = [%s]), for the reason that %s",
                        access$MethodEntity.className(), access$MethodEntity.name(), access$MethodEntity.desc(), "target is null"));
                }
            }

            return refinedInsns;
        }
    }

}
