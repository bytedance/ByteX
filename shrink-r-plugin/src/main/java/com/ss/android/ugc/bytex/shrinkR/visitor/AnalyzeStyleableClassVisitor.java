package com.ss.android.ugc.bytex.shrinkR.visitor;

import com.ss.android.ugc.bytex.shrinkR.Context;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeStyleableClassVisitor extends MethodVisitor {

    private final Context context;
    private int constVal;
    private int arraySize;
    private List<Integer> styleableValList;

    public AnalyzeStyleableClassVisitor(MethodVisitor mv, Context context) {
        super(Opcodes.ASM5, mv);
        this.context = context;
        reset();
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.ICONST_0:
                constVal = 0;
                break;
            case Opcodes.ICONST_1:
                constVal = 1;
                break;
            case Opcodes.ICONST_2:
                constVal = 2;
                break;
            case Opcodes.ICONST_3:
                constVal = 3;
                break;
            case Opcodes.ICONST_4:
                constVal = 4;
                break;
            case Opcodes.ICONST_5:
                constVal = 5;
                break;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
            constVal = operand;
        } else if (opcode == Opcodes.NEWARRAY && operand == Opcodes.T_INT) {
            arraySize = constVal;
        }
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Integer) {
            if (arraySize == -1) {
                arraySize = (int) cst;
            } else {
                styleableValList.add((Integer) cst);
            }
        }
        super.visitLdcInsn(cst);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == Opcodes.PUTSTATIC) {
            if (arraySize == styleableValList.size()) {
                context.addShouldBeInlinedRField(owner, name, styleableValList);
            } else {
                throw new RuntimeException(String.format("R styleable class [%s], field [%s] size check error:\n " +
                                "The size we calculated is %d, but in opcode size is %d",
                        owner, name, styleableValList.size(), arraySize));
            }
            reset();
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    private void reset() {
        arraySize = -1;
        styleableValList = new ArrayList<>();
    }
}
