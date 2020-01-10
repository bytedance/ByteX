package com.ss.android.ugc.bytex.closeable.visitors;

import com.ss.android.ugc.bytex.closeable.CloseableCheckContext;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Pre-scanning<br/>
 * We need to scan out some Closeable classes whose close method is empty, then we can
 * open these classes in advance without closing (because the content is empty, calling
 * and not calling are the same
 */
public class CloseableCheckPreviewClassVisitor extends BaseClassVisitor {
    private CloseableCheckContext mContext;
    private String mClassName;

    public CloseableCheckPreviewClassVisitor(CloseableCheckContext context) {
        this.mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if ("close".equals(name) && "()V".equals(desc)) {
            return new EmptyCloseMethodVisitor(methodVisitor, access, name, desc, signature, exceptions);
        }
        return methodVisitor;
    }

    class EmptyCloseMethodVisitor extends MethodNode {
        private MethodVisitor mv;

        EmptyCloseMethodVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
            super(Opcodes.ASM5, access, name, desc, signature, exceptions);
            this.mv = mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (mv != null) {
                accept(mv);
            }
            //check empty
            if (maxLocals != 1) {
                return;
            }
            if (maxStack <= 0) {
                //empty content
                mContext.addEmptyCloseable(mClassName);
            } else {
                int size = instructions.size();
                for (int i = 0; i < size; i++) {
                    AbstractInsnNode node = instructions.get(i);
                    if (node.getOpcode() > 0 && node.getOpcode() != Opcodes.RETURN) {
                        return;
                    }
                }
                mContext.addEmptyCloseable(mClassName);
            }
        }
    }
}
