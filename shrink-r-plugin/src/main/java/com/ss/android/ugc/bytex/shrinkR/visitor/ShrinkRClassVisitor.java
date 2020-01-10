package com.ss.android.ugc.bytex.shrinkR.visitor;

import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.shrinkR.Context;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ShrinkRClassVisitor extends BaseClassVisitor {
    private final Context context;
    private String className;
    private boolean isRClass;

    public ShrinkRClassVisitor(Context context) {
        this.context = context;
    }

    public ShrinkRClassVisitor(ClassVisitor cv, Context context) {
        super(cv);
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.isRClass = Utils.isRClass(name);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (isRClass && context.containRField(className, name)/* && !context.shouldKeep(className, name)*/) {
            context.getLogger().i("DeleteField", String.format("Delete field = [ %s ] in R class = [ %s ]", name, className));
            return null;
        } else if (isRClass) {
            context.getLogger().i("KeepField", String.format("Keep field = [ %s ] in R class = [ %s ]", name, className));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!isRClass) {
            return new ReplaceRFieldAccessMethodVisitor(mv, context, name, className);
        }
        return mv;
    }
}
