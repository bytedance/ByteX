package com.ss.android.ugc.bytex.getter_setter_inline.visitor;

import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.hookproguard.MethodInfo;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;

public class GetterOrSetterMethod extends MethodEntity {
    private RefFieldEntity target;
    private FieldInsnNode insn;
    private MethodInfo methodInfo;

    public GetterOrSetterMethod(String className, String name, String desc, RefFieldEntity target, FieldInsnNode insn, MethodInfo methodInfo) {
        super(Opcodes.ACC_PUBLIC, className, name, desc);
        this.target = target;
        this.insn = insn;
        this.methodInfo = methodInfo;
    }

    public RefFieldEntity getTarget() {
        return target;
    }

    public void setTarget(RefFieldEntity target) {
        this.target = target;
    }

    public FieldInsnNode getInsn() {
        return insn;
    }

    public void setInsn(FieldInsnNode insn) {
        this.insn = insn;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }
}
