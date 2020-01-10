package com.ss.android.ugc.bytex.getter_setter_inline.visitor;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.getter_setter_inline.Context;
import com.ss.android.ugc.bytex.hookproguard.ClassInfo;
import com.ss.android.ugc.bytex.hookproguard.MethodInfo;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

public class FindGetterSetterClassVisitor extends BaseClassVisitor {

    private final Context context;
    private ClassInfo classInfo;
    private boolean shouldKeep;
    private Boolean shouldKeepWholeClass;

    public FindGetterSetterClassVisitor(Context context) {
        this.context = context;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (context.isAnnotationToKeepGetterAndSetter(descriptor)) {
            shouldKeep = true;
        }
        classInfo.addAnnotation(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classInfo = new ClassInfo(access, name, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (shouldKeepWholeClass == null) {
            shouldKeepWholeClass = context.shouldKeep(classInfo);
        }
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!TypeUtil.isSynthetic(access) && !TypeUtil.isStatic(access) && !TypeUtil.isSynchronized(access)
                && !shouldKeep(classInfo.getName(), name, desc)) {
            MethodInfo methodInfo = new MethodInfo(classInfo, access, name, desc);
            mv = new ExtractFieldInsnMethodVisitor(mv, context, methodInfo);
        }
        return mv;
    }

    private boolean shouldKeep(String className, String name, String desc) {
//        if (className.startsWith("kotlin/")) return true;
        return shouldKeep || shouldKeepWholeClass || context.shouldKeep(className, name);
    }
}
