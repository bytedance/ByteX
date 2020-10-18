package com.ss.android.ugc.bytex.common.visitor;

import com.ss.android.ugc.bytex.common.Constants;

import org.objectweb.asm.ClassVisitor;

public class BaseClassVisitor extends ClassVisitor {
    public BaseClassVisitor() {
        super(Constants.ASM_API);
    }

    public BaseClassVisitor(ClassVisitor cv) {
        super(Constants.ASM_API, cv);
    }

    public void setNext(ClassVisitor cv) {
        this.cv = cv;
    }

}
