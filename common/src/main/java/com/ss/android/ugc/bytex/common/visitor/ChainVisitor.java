package com.ss.android.ugc.bytex.common.visitor;

import org.objectweb.asm.ClassVisitor;

public interface ChainVisitor {

    public void setNext(ClassVisitor cv);
}
