package com.ss.android.ugc.bytex.getter_setter_inline.visitor;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.getter_setter_inline.Context;
import com.ss.android.ugc.bytex.getter_setter_inline.ShouldSkipInlineException;
import com.ss.android.ugc.bytex.hookproguard.MethodInfo;

import org.objectweb.asm.AnnotationVisitor;
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

public class ExtractFieldInsnMethodVisitor extends MethodVisitor {

    private final Context context;
    private boolean shouldKeep;
    private final MethodInfo methodInfo;

    private List<AbstractInsnNode> instructions = new ArrayList<>();
    private static final int threshold = 16;

    public ExtractFieldInsnMethodVisitor(MethodVisitor mv, Context context, MethodInfo methodInfo) {
        super(Constants.ASM_API, mv);
        this.context = context;
        this.methodInfo = methodInfo;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (context.isAnnotationToKeepGetterAndSetter(descriptor)) {
            shouldKeep = true;
        }
        methodInfo.addAnnotation(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitInsn(final int opcode) {
        super.visitInsn(opcode);
        addInsn(new InsnNode(opcode));
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        addInsn(new IntInsnNode(opcode, operand));
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        addInsn(new VarInsnNode(opcode, var));
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        addInsn(new TypeInsnNode(opcode, type));
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        addInsn(new FieldInsnNode(opcode, owner, name, desc));
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
        addInsn(new MethodInsnNode(opcode, owner, name, desc));
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {
        if (api < Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        addInsn(new MethodInsnNode(opcode, owner, name, desc, itf));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                       Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        addInsn(new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs));
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        addInsn(new JumpInsnNode(opcode, getLabelNode(label)));
    }

    @Override
    public void visitLabel(final Label label) {
        super.visitLabel(label);
        addInsn(getLabelNode(label));
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        super.visitLdcInsn(cst);
        addInsn(new LdcInsnNode(cst));
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(var, increment);
        addInsn(new IincInsnNode(var, increment));
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
                                     final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        addInsn(new TableSwitchInsnNode(min, max, getLabelNode(dflt),
                getLabelNodes(labels)));
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
                                      final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        addInsn(new LookupSwitchInsnNode(getLabelNode(dflt), keys,
                getLabelNodes(labels)));
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        addInsn(new MultiANewArrayInsnNode(desc, dims));
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


    private void addInsn(AbstractInsnNode node) {
        if (shouldKeep) return;
        if (instructions.size() < threshold) {
            instructions.add(node);
        } else {
            shouldKeep = true;
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (shouldKeep) return;
        if (context.shouldKeep(methodInfo.getClassInfo(), methodInfo)) return;
        FieldInsnNode insnNode = extract(this.instructions);
        if (insnNode != null) {
            context.addGetterOrSetter(methodInfo.getClassInfo().getName(), methodInfo.getName(), methodInfo.getDesc(), insnNode, methodInfo);
        }
    }

    private FieldInsnNode extract(List<AbstractInsnNode> instructions) {
        FieldInsnNode fieldInstruction = null;
        int varLoadInsnCount = 0;
        try {
            for (AbstractInsnNode insnNode : instructions) {
                if (insnNode.getType() == AbstractInsnNode.LINE) continue;
                if (insnNode.getType() == AbstractInsnNode.LABEL) continue;
                if (insnNode.getOpcode() >= Opcodes.IRETURN && insnNode.getOpcode() <= Opcodes.RETURN)
                    break;
                if (fieldInstruction == null && insnNode.getOpcode() >= Opcodes.ILOAD && insnNode.getOpcode() <= Opcodes.SALOAD) {
                    varLoadInsnCount++;
                    continue;
                }

                if (fieldInstruction == null && insnNode.getType() == AbstractInsnNode.FIELD_INSN) {
                    fieldInstruction = (FieldInsnNode) insnNode;
                    int parameterCountOfMethod = TypeUtil.getParameterCountFromMethodDesc(methodInfo.getDesc());
                    switch (insnNode.getOpcode()) {
//                        case Opcodes.GETSTATIC:
//                            if (parameterCountOfMethod != 0
//                                    || varLoadInsnCount != parameterCountOfMethod) {
//                                throw new ShouldSkipInlineException("The parameter count of method is abnormal.");
//                            }
//                            break;
                        case Opcodes.GETFIELD:
                            if (parameterCountOfMethod != 0
                                    || varLoadInsnCount - 1 != parameterCountOfMethod) {
                                throw new ShouldSkipInlineException("The parameter count of method is abnormal.");
                            }
                            break;
                        case Opcodes.PUTFIELD:
                            if (parameterCountOfMethod != 1 ||
                                    varLoadInsnCount - 1 != parameterCountOfMethod) {
                                throw new ShouldSkipInlineException("The parameter count of method is abnormal.");
                            }
                            break;
//                        case Opcodes.PUTSTATIC:
//                            if (parameterCountOfMethod != 1 ||
//                                    varLoadInsnCount != parameterCountOfMethod) {
//                                throw new ShouldSkipInlineException("The parameter count of method is abnormal.");
//                            }
//                            break;
                        default:
                            throw new ShouldSkipInlineException("The instruction of method is unexpected.");
                    }
                } else {
                    throw new ShouldSkipInlineException("Unexpected instruction in method body");
                }
            }
        } catch (ShouldSkipInlineException e) {
            fieldInstruction = null;
        }
        return fieldInstruction;
    }
}
