package com.ss.android.ugc.bytex.getter_setter_inline.visitor;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.getter_setter_inline.Context;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;

public class InlineGetterSetterClassVisitor extends BaseClassVisitor {
    private static String TAG;
    private final Context context;
    private String className;

    public InlineGetterSetterClassVisitor(Context context) {
        this.context = context;
        TAG = context.extension.getName();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!TypeUtil.isPublic(access) && context.isGetterOrSetterField(className, name, desc)) {
            access = access & ~Opcodes.ACC_PRIVATE;
            access = access & ~Opcodes.ACC_PROTECTED;
            access = access | Opcodes.ACC_PUBLIC;
            context.getLogger().i("ChangeFieldToBePublic", String.format("Change Field( className = [%s], methodName = [%s], desc = [%s] ) access, from [%s] to be public",
                    className, name, desc, String.valueOf(access)));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv;
        if (context.isGetterOrSetter(className, name, desc)) {
            context.getLogger().i("DeleteGetterOrSetterMethod", String.format("Delete getter or setter method : className = [%s], methodName = [%s], desc = [%s]", className, name, desc));
            mv = null;
        } else {
            mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv = new InlineGetterSetterMethodVisitor(context, mv, className, name, desc);
        }
        return mv;
    }

    private static class InlineGetterSetterMethodVisitor extends MethodVisitor {

        private final Context context;
        private final String owner;
        private final String name;
        private final String desc;

        InlineGetterSetterMethodVisitor(Context context, MethodVisitor mv, String owner, String name, String desc) {
            super(Constants.ASM_API, mv);
            this.context = context;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            FieldInsnNode accessInsn = context.getGetterOrSetterInlineInsn(owner, name, desc);
            if (accessInsn != null) {
                accessInsn.accept(this);
                context.getLogger().i("InvokeMethod", String.format("In method( className = [%s], methodName = [%s], desc = [%s] ) code " +
                                ",inline method( className = [%s], methodName = [%s], desc = [%s] ) invoke",
                        this.owner, this.name, this.desc, owner, name, desc));
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
