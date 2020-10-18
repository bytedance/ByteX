package com.ss.android.ugc.bytex.const_inline.visitors;

import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.const_inline.ConstInlineException;
import com.ss.android.ugc.bytex.const_inline.Context;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

/**
 * const inline
 */
public class InlineConstClassVisitor extends BaseClassVisitor {
    private static final int NO_SKIP = 0;
    private static final int SKIP_NOT_STATIC = 1;
    private static final int SKIP_NOT_FINAL = 2;
    private static final int SKIP_VALUE_NULL = 3;
    private static final int SKIP_NOT_CONST = 4;
    private static final int SKIP_IN_WHITE_LIST = 5;
    private static final int SKIP_RUNTIME_CONST = 6;
    private static final int SKIP_TYPE_AUTO_FILTER_REFLECTION = 7;
    private static final int SKIP_TYPE_IN_STRING_POOL = 8;
    private static final int SKIP_TYPE_IN_ANNOTATION_CLASS = 9;
    private final Context mContext;
    private String mClassName;

    public InlineConstClassVisitor(Context context) {
        this.mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        int skip = needSkip(access, mClassName, name, desc, value);
        if (skip == NO_SKIP) {
            mContext.getLogger().i("delete const field", String.format("delete %s static final %s %s = %s", mClassName, desc, name, value));
            return null;
        } else if (skip == SKIP_TYPE_AUTO_FILTER_REFLECTION) {
            mContext.getLogger().i("skip reflect field(reflection)", String.format("skip delete %s static final %s %s = %s", mClassName, desc, name, value));
        } else if (skip == SKIP_TYPE_IN_STRING_POOL) {
            mContext.getLogger().i("skip reflect field(String)", String.format("skip delete %s static final %s %s = %s", mClassName, desc, name, value));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new InlineConstMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), name);
    }

    private int needSkip(int access, String className, String fieldName, String desc, Object value) {
        if (!TypeUtil.isStatic(access)) {
            return SKIP_NOT_STATIC;
        }
        if (!TypeUtil.isFinal(access)) {
            return SKIP_NOT_FINAL;
        }
        if (value == null) {
            return SKIP_VALUE_NULL;
        }
        if (mContext.getConstField(className, fieldName, desc, true) == null) {
            return SKIP_NOT_CONST;
        }
        if (mContext.inWhiteList(className, fieldName, desc)) {
            return SKIP_IN_WHITE_LIST;
        }
        if (mContext.isRuntimeConstField(className, fieldName, desc)) {
            return SKIP_RUNTIME_CONST;
        }
        if (mContext.inSkipAnnotationClass(className)) {
            return SKIP_TYPE_IN_ANNOTATION_CLASS;
        }
        if (mContext.extension.isAutoFilterReflectionField() && mContext.isReflectField(access, className, fieldName)) {
            return SKIP_TYPE_AUTO_FILTER_REFLECTION;
        }
        if (mContext.extension.isSupposesReflectionWithString() && mContext.inStringPool(fieldName)) {
            return SKIP_TYPE_IN_STRING_POOL;
        }
        return NO_SKIP;
    }


    private class InlineConstMethodVisitor extends MethodVisitor {
        private String methodName;

        InlineConstMethodVisitor(MethodVisitor mv, String methodName) {
            super(Constants.ASM_API, mv);
            this.methodName = methodName;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.GETSTATIC) {
                FieldNode constField = mContext.getConstField(owner, name, desc, true);
                if (constField != null && NO_SKIP == needSkip(constField.access, owner, name, desc, constField.value)) {
                    //inline const
                    mContext.getLogger().i("inline const field", String.format("change instruction in method %s.%s: GETSTATIC %s.%s to LDC %s", Utils.replaceSlash2Dot(mClassName), methodName, Utils.replaceSlash2Dot(owner), name, constField.value));
                    super.visitLdcInsn(constField.value);
                    return;
                }
            } else if (opcode == Opcodes.PUTSTATIC) {
                //check again
                FieldNode constField = mContext.getConstField(owner, name, desc, true);
                if (constField != null && NO_SKIP == needSkip(constField.access, owner, name, desc, constField.value)) {
                    throw new ConstInlineException("unexcepted situation:" + owner + "." + name + "=" + constField.value + ":" + mContext.isRuntimeConstField(owner, name, desc));
                }
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}

