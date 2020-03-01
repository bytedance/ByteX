package com.ss.android.ugc.bytex.shrinkR.visitor;

import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.shrinkR.Context;
import com.ss.android.ugc.bytex.shrinkR.exception.RFieldNotFoundException;
import com.ss.android.ugc.bytex.shrinkR.res_check.AssetsManager;
import com.ss.android.ugc.bytex.shrinkR.res_check.ResManager;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

class ReplaceRFieldAccessMethodVisitor extends MethodVisitor {
    private final Context context;
    private final ResManager resManager;
    private final AssetsManager assetsManager;
    private String methodName;
    private String className;
    private int processingLineNumber;

    public ReplaceRFieldAccessMethodVisitor(MethodVisitor mv, Context context, String methodName, String className) {
        super(Opcodes.ASM5, mv);
        this.context = context;
        this.methodName = methodName;
        this.className = className;
        this.resManager = context.getChecker().getResManager();
        this.assetsManager = context.getChecker().getAssetsManager();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == Opcodes.GETSTATIC) {
            Object value = null;
            try {
                value = context.getRFieldValue(owner, name);
            } catch (RFieldNotFoundException e) {
                context.addNotFoundRField(className, methodName, owner, name);
            }
            if (value != null) {
                if (value instanceof List) {
                    replaceStyleableNewArrayCode((List<Integer>) value);
                } else if (value instanceof Integer) {
                    resManager.reachResource((Integer) value);
                    mv.visitLdcInsn(value);
                }
                return;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        processingLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type) {
            int sort = ((Type) value).getSort();
            if (sort == Type.OBJECT) {
                Type type = (Type) value;
                String rClassName = type.getInternalName();
                if (Utils.isRClass(rClassName)) {
                    StringBuilder sb = new StringBuilder();
                    String msg = String.format("R class = [ %s ] may be references by reflect api, please check if it has bean kept.\n", rClassName);
                    sb.append(msg)
                            .append(String.format("             at %s.%s(%s.java:%s) \n",
                                    className.replaceAll("/", "."), methodName, className.replaceAll("/", "."), String.valueOf(processingLineNumber)));
                    context.getLogger().w(context.extension.getName(), sb.toString(), null);
                }
            }
        } else if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 1) {
                if (Utils.isRClassName(str)) {
                    StringBuilder sb = new StringBuilder();
                    String msg = String.format("R class = [ %s ] may be references by reflect api, please check if it has bean kept.\n", str);
                    sb.append(msg)
                            .append(String.format("             at %s.%s(%s.java:%s) \n",
                                    className.replaceAll("/", "."), methodName, className.replaceAll("/", "."), String.valueOf(processingLineNumber)));
                    context.getLogger().w(context.extension.getName(), sb.toString(), null);
                }
                assetsManager.tryReachAsset(str);
            }
        } else if (value instanceof Integer) {
            resManager.reachResource((Integer) value);
        }
        super.visitLdcInsn(value);
    }

    private void replaceStyleableNewArrayCode(List<Integer> valList) {
        int size = valList.size();
        visitConstInsByVal(mv, size);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
        for (int i = 0; i < size; i++) {
            mv.visitInsn(Opcodes.DUP);
            visitConstInsByVal(mv, i);
            mv.visitLdcInsn(valList.get(i));
            mv.visitInsn(Opcodes.IASTORE);
        }
    }

    private void visitConstInsByVal(MethodVisitor mv, int constVal) {
        int opcodes;
        if (constVal == 0) {
            opcodes = Opcodes.ICONST_0;
        } else if (constVal == 1) {
            opcodes = Opcodes.ICONST_1;
        } else if (constVal == 2) {
            opcodes = Opcodes.ICONST_2;
        } else if (constVal == 3) {
            opcodes = Opcodes.ICONST_3;
        } else if (constVal == 4) {
            opcodes = Opcodes.ICONST_4;
        } else if (constVal == 5) {
            opcodes = Opcodes.ICONST_5;
        } else if (constVal < 128) {
            opcodes = Opcodes.BIPUSH;
        } else if (constVal < 32768) {
            opcodes = Opcodes.SIPUSH;
        } else {
            opcodes = Opcodes.LDC;
        }
        if (opcodes == Opcodes.LDC) {
            mv.visitLdcInsn(constVal);
        }
        if (opcodes == Opcodes.SIPUSH || opcodes == Opcodes.BIPUSH) {
            mv.visitIntInsn(opcodes, constVal);
        } else {
            mv.visitInsn(opcodes);
        }
    }
}
