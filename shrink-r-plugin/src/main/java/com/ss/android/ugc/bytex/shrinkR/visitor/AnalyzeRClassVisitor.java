package com.ss.android.ugc.bytex.shrinkR.visitor;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.shrinkR.Context;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class AnalyzeRClassVisitor extends BaseClassVisitor {
    private final Context context;
    private String className;
    private boolean discardable = true;
    private boolean isRStyleableClass = false;

    public AnalyzeRClassVisitor(Context context) {
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.isRStyleableClass = Utils.isRStyleableClass(context.getRealRClassName(className));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (TypeUtil.isPublic(access) && TypeUtil.isStatic(access) && TypeUtil.isFinal(access)
                && !context.shouldKeep(this.className, name)) {
            if (TypeUtil.isInt(desc) && value != null) {
                context.addShouldBeInlinedRField(className, name, value);
            }
        } else {
            discardable = false;
            context.addSkipInlineRField(className, name, value);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (this.isRStyleableClass && Utils.isClassInit(name)) {
            if (discardable) {
                return new AnalyzeStyleableClassVisitor(mv, context);
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (discardable) {
            context.addShouldDiscardRClasses(className);
        }
    }
}
