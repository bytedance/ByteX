package com.ss.android.ugc.bytex.access_inline.visitor;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.MemberType;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.access_inline.Context;
import com.ss.android.ugc.bytex.access_inline.ShouldSkipInlineException;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

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
            return new RefineAccess$MethodVisitor(mv, context, access$MethodEntity);
        }
        return mv;
    }

    static class RefineAccess$MethodVisitor extends MethodVisitor {

        private List<AbstractInsnNode> instructions = new ArrayList<>();
        private Access$MethodEntity access$MethodEntity;
        private Context context;

        public RefineAccess$MethodVisitor(MethodVisitor mv, Context context, Access$MethodEntity access$MethodEntity) {
            super(Constants.ASM_API, mv);
            this.access$MethodEntity = access$MethodEntity;
            this.context = context;
        }


        @Override
        public void visitInsn(final int opcode) {
            super.visitInsn(opcode);
            instructions.add(new InsnNode(opcode));
        }

        @Override
        public void visitIntInsn(final int opcode, final int operand) {
            super.visitIntInsn(opcode, operand);
            instructions.add(new IntInsnNode(opcode, operand));
        }

        @Override
        public void visitVarInsn(final int opcode, final int var) {
            super.visitVarInsn(opcode, var);
            instructions.add(new VarInsnNode(opcode, var));
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            super.visitTypeInsn(opcode, type);
            instructions.add(new TypeInsnNode(opcode, type));
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner,
                                   final String name, final String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);
            instructions.add(new FieldInsnNode(opcode, owner, name, desc));
        }

        @Deprecated
        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String desc) {
            if (api >= Opcodes.ASM5) {
                super.visitMethodInsn(opcode, owner, name, desc);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc);
            instructions.add(new MethodInsnNode(opcode, owner, name, desc));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String desc, boolean itf) {
            if (api < Opcodes.ASM5) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            instructions.add(new MethodInsnNode(opcode, owner, name, desc, itf));
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                           Object... bsmArgs) {
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            instructions.add(new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs));
        }

        @Override
        public void visitJumpInsn(final int opcode, final Label label) {
            super.visitJumpInsn(opcode, label);
            instructions.add(new JumpInsnNode(opcode, getLabelNode(label)));
        }

        @Override
        public void visitLabel(final Label label) {
            super.visitLabel(label);
            instructions.add(getLabelNode(label));
        }

        @Override
        public void visitLdcInsn(final Object cst) {
            super.visitLdcInsn(cst);
            instructions.add(new LdcInsnNode(cst));
        }

        @Override
        public void visitIincInsn(final int var, final int increment) {
            super.visitIincInsn(var, increment);
            instructions.add(new IincInsnNode(var, increment));
        }

        @Override
        public void visitTableSwitchInsn(final int min, final int max,
                                         final Label dflt, final Label... labels) {
            super.visitTableSwitchInsn(min, max, dflt, labels);
            instructions.add(new TableSwitchInsnNode(min, max, getLabelNode(dflt),
                    getLabelNodes(labels)));
        }

        @Override
        public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
                                          final Label[] labels) {
            super.visitLookupSwitchInsn(dflt, keys, labels);
            instructions.add(new LookupSwitchInsnNode(getLabelNode(dflt), keys,
                    getLabelNodes(labels)));
        }

        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            super.visitMultiANewArrayInsn(desc, dims);
            instructions.add(new MultiANewArrayInsnNode(desc, dims));
        }

        protected LabelNode getLabelNode(final Label l) {
            if (!(l.info instanceof LabelNode)) {
                l.info = new LabelNode();
            }
            return (LabelNode) l.info;
        }

        private LabelNode[] getLabelNodes(final Label[] l) {
            LabelNode[] nodes = new LabelNode[l.length];
            for (int i = 0; i < l.length; ++i) {
                nodes[i] = getLabelNode(l[i]);
            }
            return nodes;
        }

        private Object[] getLabelNodes(final Object[] objs) {
            Object[] nodes = new Object[objs.length];
            for (int i = 0; i < objs.length; ++i) {
                Object o = objs[i];
                if (o instanceof Label) {
                    o = getLabelNode((Label) o);
                }
                nodes[i] = o;
            }
            return nodes;
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

        private List<AbstractInsnNode> refine(List<AbstractInsnNode> instructions) {
            List<AbstractInsnNode> refinedInsns = new ArrayList<>();
            boolean shouldSkipVarInsn = true;
            int varLoadInsnCount = 0;
            MemberEntity target = null;
            try {
                for (AbstractInsnNode insnNode : instructions) {
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
            return refinedInsns;
        }
    }
}
