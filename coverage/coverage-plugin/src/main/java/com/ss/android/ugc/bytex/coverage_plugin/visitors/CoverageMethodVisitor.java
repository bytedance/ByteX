package com.ss.android.ugc.bytex.coverage_plugin.visitors;

import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.Constants;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;


public class CoverageMethodVisitor extends MethodNode {

    private BaseContext context;
    private final int mapping;
    private String className;

    private MethodVisitor mv;

    public CoverageMethodVisitor( int access, String name, String desc, String signature, String[] exceptions, BaseContext context, int mapping, String className, MethodVisitor mv) {
        super(Constants.ASM_API, access, name, desc, signature, exceptions);
        this.context = context;
        this.mapping = mapping;
        this.className = className;
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (this.mv == null) {
            return;
        }
        if (instructions != null) {
            boolean needInsert = true;
            // static块一定插桩
            if (!name.equals("<clinit>")){
                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode ins = instructions.get(i);
                    if (Opcodes.INVOKESPECIAL == ins.getOpcode()) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) ins;
                        if (methodInsnNode.owner.equals(className) && methodInsnNode.name.equals("<init>")) {
                            // skip if contains this(...)
                            needInsert = false;
                            break;
                        }
                    }
                }
            }
            if (needInsert) {
                instructions.insertBefore(instructions.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, "com/ss/android/ugc/bytex/coverage_lib/CoverageLogger", "Log", "(I)V", false));
                instructions.insertBefore(instructions.get(0), new LdcInsnNode(mapping));
            }
        }

        accept(this.mv);
    }

}
