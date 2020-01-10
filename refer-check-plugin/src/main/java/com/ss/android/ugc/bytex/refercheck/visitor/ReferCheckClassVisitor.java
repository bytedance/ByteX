package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.refercheck.ReferCheckContext;

import org.objectweb.asm.MethodVisitor;

/**
 * Created by tanlehua on 2018/2/6.
 * 1. 检查每一次方法调用是否存在对应的方法和相应的类
 */

public class ReferCheckClassVisitor extends BaseClassVisitor {
    private final ReferCheckContext context;
    private String className;
    private boolean isInterface;
    //    private boolean shouldCheck;
    private String sourceFile;

    public ReferCheckClassVisitor(ReferCheckContext context) {
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = TypeUtil.isInterface(access);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (!isInterface && !TypeUtil.isAbstract(access) && !TypeUtil.isSynthetic(access) && context.shouldCheck(className, methodName)) {
            mv = new ReferCheckMethodVisitor(mv, context, methodName, className, sourceFile);
        }
        return mv;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.sourceFile = source;
    }
}
