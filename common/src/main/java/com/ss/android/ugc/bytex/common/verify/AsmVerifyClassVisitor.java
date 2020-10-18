package com.ss.android.ugc.bytex.common.verify;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Created by yangzhiqian on 2019/4/18<br/>
 * Desc:a ClassVisitor that verifies the class method
 */
public class AsmVerifyClassVisitor extends BaseClassVisitor {
    private String className;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new AsmVerifyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions),
                access, name, desc, signature, exceptions);

    }

    private class AsmVerifyMethodVisitor extends MethodNode {
        private MethodVisitor mv;

        AsmVerifyMethodVisitor(MethodVisitor mv, final int access, final String name, final String desc,
                               final String signature, final String[] exceptions) {
            super(Constants.ASM_API, access, name, desc, signature, exceptions);
            this.mv = mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            AsmVerifier.verify(className, this);
            if (this.mv != null) {
                accept(this.mv);
            }
        }
    }
}
