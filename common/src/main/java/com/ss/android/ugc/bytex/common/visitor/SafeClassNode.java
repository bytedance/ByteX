package com.ss.android.ugc.bytex.common.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Created by tanlehua on 2019/4/24.
 */
public class SafeClassNode extends ClassNode {

    public SafeClassNode() {
        super(Opcodes.ASM5);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodNode method = new SafeMethodNode(access, name, descriptor, signature, exceptions);
        methods.add(method);
        return method;
    }
}
