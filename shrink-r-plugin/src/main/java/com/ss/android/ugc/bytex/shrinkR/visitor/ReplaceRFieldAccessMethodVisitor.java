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
                if (value instanceof Integer) {
                    resManager.reachResource((Integer) value);
                }
                mv.visitLdcInsn(value);
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
}
