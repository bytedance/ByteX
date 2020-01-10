package com.ss.android.ugc.bytex.common.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class BaseClassVisitor extends ClassVisitor {
    public BaseClassVisitor() {
        super(Opcodes.ASM5);
    }

    public BaseClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    public void setNext(ClassVisitor cv) {
        this.cv = cv;
    }

}
