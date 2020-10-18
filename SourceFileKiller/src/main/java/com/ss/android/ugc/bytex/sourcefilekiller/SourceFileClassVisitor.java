package com.ss.android.ugc.bytex.sourcefilekiller;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SourceFileClassVisitor extends BaseClassVisitor {
    private SourceFileExtension extension;

    public SourceFileClassVisitor(SourceFileExtension extension) {
        this.extension = extension;
    }

    @Override
    public void visitSource(String source, String debug) {
        if (extension.isDeleteSourceFile()) {
            // delete
            return;
        }
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (extension.isDeleteLineNumber()) {
            return new MethodVisitor(Constants.ASM_API, mv) {
                @Override
                public void visitLineNumber(int line, Label start) {
                    //delete
                }
            };
        }
        return mv;
    }
}
