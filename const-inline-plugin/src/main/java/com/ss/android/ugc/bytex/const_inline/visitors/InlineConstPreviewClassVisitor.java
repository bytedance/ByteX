package com.ss.android.ugc.bytex.const_inline.visitors;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.const_inline.Context;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

/**
 * Pre-Scanning<br/>
 * Find all constants and exclude which constants are assigned during runtime
 */
public class InlineConstPreviewClassVisitor extends BaseClassVisitor {
    private Context mContext;
    private String mClassName;

    public InlineConstPreviewClassVisitor(Context context) {
        this.mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (mContext.inSkipWithAnnotations(desc)) {
            mContext.addSkipAnnotationClass(mClassName);
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fieldVisitor = super.visitField(access, name, desc, signature, value);
        if (TypeUtil.isStatic(access) && TypeUtil.isFinal(access)) {
            mContext.addConstField(mClassName, access, name, desc, signature, value);
            return new RuntimeConstFieldScanFieldVisitor(name, desc, fieldVisitor);
        }
        return fieldVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //We need to find all fields marked as final but still assigned in the method
        return new RuntimeConstFieldScanMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    class RuntimeConstFieldScanMethodVisitor extends MethodVisitor {

        RuntimeConstFieldScanMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.PUTSTATIC) {
                FieldNode constField = mContext.getConstField(owner, name, desc, false);
                if (constField != null) {
                    mContext.addRuntimeConstField(owner, name, desc);
                }
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            super.visitLdcInsn(cst);
            if (mContext.extension.isSupposesReflectionWithString() && cst instanceof String) {
                mContext.addString((String) cst);
            }
        }
    }

    class RuntimeConstFieldScanFieldVisitor extends FieldVisitor {
        private final String mFieldName;
        private final String mFieldDesc;

        RuntimeConstFieldScanFieldVisitor(String name, String desc, FieldVisitor fieldVisitor) {
            super(Opcodes.ASM5, fieldVisitor);
            this.mFieldName = name;
            this.mFieldDesc = desc;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if ((mContext.extension.isSkipWithRuntimeAnnotation() && visible) || mContext.inSkipWithAnnotations(desc)) {
                FieldNode constField = mContext.getConstField(mClassName, mFieldName, mFieldDesc, false);
                if (constField != null) {
                    mContext.addRuntimeConstField(mClassName, mFieldName, mFieldDesc);
                }
            }
            return super.visitAnnotation(desc, visible);
        }
    }
}
