package com.ss.android.ugc.bytex.access_inline.visitor;


import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.RefMemberEntity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class Access$MethodEntity extends MethodEntity {
    private RefMemberEntity target;
    private List<AbstractInsnNode> insnNodeList;

    public Access$MethodEntity(String className, String name, String desc) {
        super(Opcodes.ACC_STATIC, className, name, desc);
    }

    public RefMemberEntity getTarget() {
        return target;
    }

    public void setTarget(RefMemberEntity target) {
        this.target = target;
    }

    public List<AbstractInsnNode> getInsnNodeList() {
        return insnNodeList;
    }

    public void setInsnNodeList(List<AbstractInsnNode> insnNodeList) {
        this.insnNodeList = insnNodeList;
    }

    public MethodInsnNode getMethodInsn() {
        if (target instanceof RefFieldEntity) {
            return null;
        }
        for (AbstractInsnNode insnNode : insnNodeList) {
            if (insnNode instanceof MethodInsnNode) {
                return (MethodInsnNode) insnNode;
            }
        }
        return null;
    }

    public FieldInsnNode getFieldInsn() {
        if (target instanceof RefMethodEntity) {
            return null;
        }
        for (AbstractInsnNode insnNode : insnNodeList) {
            if (insnNode instanceof FieldInsnNode) {
                return (FieldInsnNode) insnNode;
            }
        }
        return null;
    }
}
