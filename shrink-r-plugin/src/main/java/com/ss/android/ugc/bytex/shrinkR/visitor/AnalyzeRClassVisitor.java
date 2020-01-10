package com.ss.android.ugc.bytex.shrinkR.visitor;

import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.shrinkR.Context;

import org.objectweb.asm.FieldVisitor;

public class AnalyzeRClassVisitor extends BaseClassVisitor {
    private final Context context;
    private String className;
    private boolean discardable = true;

    public AnalyzeRClassVisitor(Context context) {
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (TypeUtil.isPublic(access) && TypeUtil.isStatic(access) && TypeUtil.isFinal(access) && TypeUtil.isInt(desc)
                && !context.shouldKeep(this.className, name)) {
            context.addRField(className, name, value);
        } else {
            discardable = false;
            context.addSkipRField(className, name, value);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (discardable) {
            context.addRClass(className);
        }
    }
}
