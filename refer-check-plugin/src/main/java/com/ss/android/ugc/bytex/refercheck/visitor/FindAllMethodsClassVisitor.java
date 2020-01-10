package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.refercheck.ReferCheckContext;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by tanlehua on 2018/2/6.
 * 1. 检查每一次方法调用是否存在对应的方法和相应的类
 */
@Deprecated
public class FindAllMethodsClassVisitor extends BaseClassVisitor {
    private final ReferCheckContext context;
    private String className;
    private boolean isInterface;
    private boolean shouldCheck;

    public FindAllMethodsClassVisitor(ReferCheckContext context) {
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = TypeUtil.isInterface(access);
        this.shouldCheck = context.shouldCheck(className);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        if (shouldCheck && !isInterface && !TypeUtil.isAbstract(access)) {
            context.addMethod(className, methodName, desc);
        }
        return super.visitMethod(access, methodName, desc, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access, String fieldName, String desc, String signature, Object value) {
        if (shouldCheck && !TypeUtil.isAbstract(access)) {
            context.addField(className, fieldName, desc);
        }
        return super.visitField(access, fieldName, desc, signature, value);
    }
}
