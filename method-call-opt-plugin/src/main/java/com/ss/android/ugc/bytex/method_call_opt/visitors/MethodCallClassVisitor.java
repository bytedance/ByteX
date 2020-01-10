package com.ss.android.ugc.bytex.method_call_opt.visitors;

import com.ss.android.ugc.bytex.common.utils.OpcodesUtils;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.method_call_opt.MethodCallOptContext;
import com.ss.android.ugc.bytex.method_call_opt.MethodCallOptException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


/**
 * Created by yangzhiqian on 2019/3/15<br/>
 * Desc: opt method call
 */
public class MethodCallClassVisitor extends BaseClassVisitor {
    private static final int SKIP_INDEX_FRAME = -1;
    private static final int SKIP_INDEX_IF = -2;
    private static final int SKIP_INDEX_JSR = -3;
    private static final int SKIP_INDEX_GOTO = -4;
    private static final int SKIP_INDEX_RETURN = -5;
    private static final int SKIP_INDEX_ATHROW = -6;
    private static final int SKIP_INDEX_SWITCH = -7;
    private static final Type OBJECT_TYPE = Type.getType(Object.class);//any reference type. include array.
    private static final Type SLOT_TYPE = Type.getType("LSlot;");//Operand that occupies a slot
    private static final Type DOUBLE_SLOT_TYPE = Type.getType("LSlot2;");//Operand that occupies two slots
    private MethodCallOptContext mContext;
    private String mClassName;
    private String mSimpleClassName;
    //top outer class
    private String mOutSimpleClassName;


    public MethodCallClassVisitor(MethodCallOptContext context) {
        mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.mClassName = name;
        mSimpleClassName = mClassName.substring(mClassName.lastIndexOf("/") + 1);
        if (mSimpleClassName.contains("$")) {
            mOutSimpleClassName = mSimpleClassName.substring(0, mSimpleClassName.indexOf("$"));
        }

    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (mContext.needCheck(mClassName, name, desc)) {
            return new MethodCallOptMethodVisitor(methodVisitor, access, name, desc, signature, exceptions);
        }
        return methodVisitor;
    }

    class MethodCallOptMethodVisitor extends MethodNode {

        private MethodVisitor mMv;
        private String mMethodName;

        MethodCallOptMethodVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
            super(Opcodes.ASM5, access, name, desc, signature, exceptions);
            this.mMv = mv;
            this.mMethodName = name;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();

            //reverse analysis
            int index = instructions.size() - 1;
            //collection for nodes which will be removed
            List<List<AbstractInsnNode>> optimizedIns = new ArrayList<>();
            while (index >= 0) {
                AbstractInsnNode node = instructions.get(index);
                if (node.getOpcode() == Opcodes.INVOKESTATIC ||
                        node.getOpcode() == Opcodes.INVOKESPECIAL ||
                        node.getOpcode() == Opcodes.INVOKEVIRTUAL ||
                        node.getOpcode() == Opcodes.INVOKEINTERFACE
                ) {
                    //optimize method calls only.INVOKEDYNAMIC excluded
                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                    final boolean isStatic = methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC;
                    //we have collect all method which contains method call which need to be optimized.check it directly to improve efficiency
                    if (mContext.isOptimizationNeededMethod(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, isStatic)) {
                        Type returnType = Type.getReturnType(methodInsnNode.desc);
                        boolean needOpt = false;
                        //returnType must be void or value must not be used
                        if (Type.VOID_TYPE.equals(returnType)) {
                            needOpt = true;
                        } else if (typeOf(returnType, SLOT_TYPE)) {
                            needOpt = index + 1 < instructions.size() && instructions.get(index + 1).getOpcode() == Opcodes.POP;
                        } else if (typeOf(returnType, DOUBLE_SLOT_TYPE)) {
                            needOpt = index + 1 < instructions.size() && instructions.get(index + 1).getOpcode() == Opcodes.POP2;
                        }
                        if (needOpt) {
                            mParamsStack.clear();
                            if (!isStatic) {
                                //put this-owner
                                push(Type.getObjectType(methodInsnNode.owner));
                            }
                            //put arguments
                            Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                            for (Type argumentType : argumentTypes) {
                                push(argumentType);
                            }
                            try {
                                //find the start instruction index of the method call
                                int succeedIndex = optimize(index);
                                if (succeedIndex >= 0) {
                                    List<AbstractInsnNode> removedInsnNode = new ArrayList<>();
                                    if (!Type.VOID_TYPE.equals(returnType)) {
                                        //remove pop or pop2
                                        index++;
                                    }
                                    /*
                                     * There must be at least one valid instruction(which opcode >=0.)
                                     * between the two FrameNode.Otherwise, an IllegalStateException
                                     * will be thrown in the MethodWriter's visitFrame method.
                                     * So we should delete the FrameNode which in front of the method
                                     * call instruction if there no valid instruction before the next
                                     * FrameNode
                                     */
                                    boolean needRemoveFrame = false;
                                    for (int next = index + 1; next < instructions.size(); next++) {
                                        AbstractInsnNode nextNode = instructions.get(next);
                                        boolean insNodexRemoved = false;
                                        for (List<AbstractInsnNode> optimizedIn : optimizedIns) {
                                            if (optimizedIn.contains(nextNode)) {
                                                insNodexRemoved = true;
                                                break;
                                            }
                                        }
                                        if (insNodexRemoved) {
                                            continue;
                                        }
                                        if (nextNode.getOpcode() >= 0) {
                                            break;
                                        }
                                        if (nextNode instanceof FrameNode) {
                                            //find it;
                                            needRemoveFrame = true;
                                            break;
                                        }
                                    }
                                    if (needRemoveFrame) {
                                        //Find the previous FrameNode index
                                        for (int frameNodeIndex = succeedIndex - 1; frameNodeIndex >= 0; frameNodeIndex--) {
                                            AbstractInsnNode lastNode = instructions.get(frameNodeIndex);
                                            if (lastNode.getOpcode() >= 0 || lastNode instanceof LabelNode) {
                                                break;
                                            }
                                            if (lastNode instanceof FrameNode) {
                                                //find it;
                                                succeedIndex = frameNodeIndex;
                                                break;
                                            }
                                        }
                                    }
                                    while (index >= succeedIndex) {
                                        removedInsnNode.add(instructions.get(index--));
                                    }
                                    optimizedIns.add(removedInsnNode);
                                    continue;
                                } else {
                                    mContext.getLogger().i("skip opt", generateStackTraceString(index) + " :return " + succeedIndex + "\nparamStack=" + getStackString(mParamsStack));
                                }
                            } catch (MethodCallOptException e) {
                                mContext.getLogger().w("MethodCallOptException", generateStackTraceString(index) + " :" + e.toString() + "\nparamStack=" + getStackString(mParamsStack));
                            } catch (AssertionError error) {
                                mContext.getLogger().w("AssertionError", generateStackTraceString(index) + " :" + error.toString() + "\nparamStack=" + getStackString(mParamsStack));
                            }
                        } else {
                            mContext.getLogger().i("skip opt", generateStackTraceString(index));
                        }
                    }
                }
                index--;
            }
            if (optimizedIns.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (List<AbstractInsnNode> optimizedIn : optimizedIns) {
                    stringBuilder.append("remove instructions:").append(optimizedIn.size()).append("   LineNumber=").append(getLineNumber(instructions.indexOf(optimizedIn.get(optimizedIn.size() - 1)))).append("\n");
                    //reverse delete
                    for (int i = optimizedIn.size() - 1; i >= 0; i--) {
                        AbstractInsnNode node = optimizedIn.get(i);
                        instructions.remove(node);
                        stringBuilder.append(OpcodesUtils.covertToString(node)).append("\n");
                    }
                    stringBuilder.append("\n");
                }
                if (mContext.extension.isShowAfterOptInsLog()) {
                    stringBuilder.append("\n\n\nafter opt\n");
                    int size = instructions.size();
                    for (int i = 0; i < size; i++) {
                        stringBuilder.append(OpcodesUtils.covertToString(instructions.get(i))).append("\n");
                    }
                }
                mContext.getLogger().i(Utils.replaceSlash2Dot(mClassName) + "." + mMethodName, stringBuilder.toString());
            }
            if (this.mMv != null) {
                accept(this.mMv);
            }
        }

        private final Stack<Type> mParamsStack = new Stack<>();

        private Type pop() {
            return mParamsStack.pop();
        }

        private void push(Type type) {
            mParamsStack.push(type);
        }

        private String generateStackTraceString(int insIndex) {
            return Utils.replaceSlash2Dot(mClassName) + "." + mMethodName + "(" + (mOutSimpleClassName == null ? mSimpleClassName : mOutSimpleClassName) + ".java:" + getLineNumber(insIndex) + ")";
        }

        /**
         * calculate the start instruction index of the operand stack required to execute the current instruction
         *
         * @param index Index of current instruction
         * @return The index of the instruction to start pushing the operand. <0 indicates that the situation was ignored.
         */
        private int optimize(final int index) throws MethodCallOptException {
            if (mParamsStack.size() == 0) {
                return index;
            }
            if (index <= 0) {
                throw new MethodCallOptException("Can not match the method call params:index=" + index);
            }
            final int next = index - 1;
            final AbstractInsnNode node = instructions.get(next);
            if (node.getOpcode() < 0) {
                if (node instanceof LineNumberNode || node instanceof LabelNode) {
                    //LABEL or LINENUMBER...
                    return optimize(next);
                } else {
                    //frame
                    return SKIP_INDEX_FRAME;
                }

            }
            Type pop1, pop2, pop3, pop4, pop5, pop6;
            final Type type = mParamsStack.peek();
            //boolean byte char short ->int
            switch (node.getOpcode()) {
                case Opcodes.NOP:
                    return optimize(next);
                case Opcodes.ACONST_NULL:
                    if (typeOf(OBJECT_TYPE, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a null object", type.getClassName()));
                    }
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH:
                case Opcodes.ILOAD:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.LLOAD:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.FLOAD:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                case Opcodes.DLOAD:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.LDC:
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) node;
                    if (ldcInsnNode.cst instanceof Integer && typeOf(Type.INT_TYPE, type)) {
                        //lcd int
                        pop();
                        return optimize(next);
                    } else if (ldcInsnNode.cst instanceof Float && typeOf(Type.FLOAT_TYPE, type)) {
                        //lcd float
                        pop();
                        return optimize(next);
                    } else if (ldcInsnNode.cst instanceof Long && typeOf(Type.LONG_TYPE, type)) {
                        //lcd long
                        pop();
                        return optimize(next);
                    } else if (ldcInsnNode.cst instanceof Double && typeOf(Type.DOUBLE_TYPE, type)) {
                        //lcd double
                        pop();
                        return optimize(next);
                    } else if (ldcInsnNode.cst instanceof String && typeOf(Type.getType(String.class), type)) {
                        //lcd string
                        pop();
                        return optimize(next);
                    } else if (ldcInsnNode.cst instanceof Type && typeOf(Type.getType(Class.class), type)) {
                        //getClass
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), ldcInsnNode.cst.getClass().getName()));
                    }
//              case Opcodes.LDC_W:break;
//              case Opcodes.LDC2_W:break;
                case Opcodes.ALOAD:
                    pop();
                    return optimize(next);
//                    VarInsnNode varInsnNode = (VarInsnNode) node;
//                    LocalVariableNode localVar = getLocalVar(varInsnNode.var, next);
//                    if (localVar == null) {
//                        throw new MethodCallOptException("Can not find the local var！index=" + varInsnNode.var);
//                    }
//                    Type varType = Type.getType(localVar.desc);
//                    if (typeOf(varType, type)) {
//                        pop();
//                        return optimize(next);
//                    } else {
//                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), varType.getDescriptor()));
//                    }
//              case Opcodes.ILOAD_0:break;
//              case Opcodes.ILOAD_1:break;
//              case Opcodes.ILOAD_2:break;
//              case Opcodes.ILOAD_3:break;
//              case Opcodes.LLOAD_0:break;
//              case Opcodes.LLOAD_1:break;
//              case Opcodes.LLOAD_2:break;
//              case Opcodes.LLOAD_3:break;
//              case Opcodes.FLOAD_0:break;
//              case Opcodes.FLOAD_1:break;
//              case Opcodes.FLOAD_2:break;
//              case Opcodes.FLOAD_3:break;
//              case Opcodes.DLOAD_0:break;
//              case Opcodes.DLOAD_1:break;
//              case Opcodes.DLOAD_2:break;
//              case Opcodes.DLOAD_3:break;
//              case Opcodes.ALOAD_0:break;
//              case Opcodes.ALOAD_1:break;
//              case Opcodes.ALOAD_2:break;
//              case Opcodes.ALOAD_3:break;
                case Opcodes.IALOAD:
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.SALOAD:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.getType("[I"));
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.LALOAD:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.getType("[J"));
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.FALOAD:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        push(Type.getType("[F"));
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.DALOAD:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        push(Type.getType("[D"));
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.AALOAD:
                    if (typeOf(OBJECT_TYPE, type)) {
                        pop();
                        if (type == SLOT_TYPE || type == OBJECT_TYPE) {
                            push(OBJECT_TYPE);
                            mContext.getLogger().d("AALOAD=" + type.getDescriptor());
                        } else {
                            push(Type.getType("[" + type.getDescriptor()));
                        }
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a object or an array value", type.getDescriptor()));
                    }
                case Opcodes.ISTORE:
                    push(Type.INT_TYPE);
                    return optimize(next);
                case Opcodes.LSTORE:
                    push(Type.LONG_TYPE);
                    return optimize(next);
                case Opcodes.FSTORE:
                    push(Type.FLOAT_TYPE);
                    return optimize(next);
                case Opcodes.DSTORE:
                    push(Type.DOUBLE_TYPE);
                    return optimize(next);
                case Opcodes.ASTORE:
                    push(OBJECT_TYPE);
                    return optimize(next);
//              case Opcodes.ISTORE_0:break;
//              case Opcodes.ISTORE_1:break;
//              case Opcodes.ISTORE_2:break;
//              case Opcodes.ISTORE_3:break;
//              case Opcodes.LSTORE_0:break;
//              case Opcodes.LSTORE_1:break;
//              case Opcodes.LSTORE_2:break;
//              case Opcodes.LSTORE_3:break;
//              case Opcodes.FSTORE_0:break;
//              case Opcodes.FSTORE_1:break;
//              case Opcodes.FSTORE_2:break;
//              case Opcodes.FSTORE_3:break;
//              case Opcodes.DSTORE_0:break;
//              case Opcodes.DSTORE_1:break;
//              case Opcodes.DSTORE_2:break;
//              case Opcodes.DSTORE_3:break;
//              case Opcodes.ASTORE_0:break;
//              case Opcodes.ASTORE_1:break;
//              case Opcodes.ASTORE_2:break;
//              case Opcodes.ASTORE_3:break;
                case Opcodes.IASTORE:
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE:
                    push(Type.getType("[I"));
                    push(Type.INT_TYPE);
                    push(Type.INT_TYPE);
                    return optimize(next);
                case Opcodes.LASTORE:
                    push(Type.getType("[J"));
                    push(Type.INT_TYPE);
                    push(Type.LONG_TYPE);
                    return optimize(next);
                case Opcodes.FASTORE:
                    push(Type.getType("[F"));
                    push(Type.INT_TYPE);
                    push(Type.FLOAT_TYPE);
                    return optimize(next);
                case Opcodes.DASTORE:
                    push(Type.getType("[D"));
                    push(Type.INT_TYPE);
                    push(Type.DOUBLE_TYPE);
                    return optimize(next);
                case Opcodes.AASTORE:
                    //We can't clarify the specific type of array here.so we use object
                    push(OBJECT_TYPE);
                    push(Type.INT_TYPE);
                    push(OBJECT_TYPE);
                    return optimize(next);
                case Opcodes.POP:
                    push(SLOT_TYPE);
                    return optimize(next);
                case Opcodes.POP2:
                    push(DOUBLE_SLOT_TYPE);
                    return optimize(next);
                case Opcodes.DUP:
                    pop1 = pop();
                    assert typeOf(pop1, SLOT_TYPE);
                    return optimize(next);
                case Opcodes.DUP_X1:
                    pop1 = pop();
                    assert typeOf(pop1, SLOT_TYPE);
                    pop2 = pop();
                    assert typeOf(pop2, SLOT_TYPE);
                    pop3 = pop();
                    assert typeOf(pop3, pop1);
                    push(pop2);
                    push(pop1);
                    return optimize(next);
                case Opcodes.DUP_X2:
                    pop1 = pop();
                    assert typeOf(pop1, SLOT_TYPE);
                    pop2 = pop();
                    if (typeOf(pop2, SLOT_TYPE)) {
                        pop3 = pop();
                        assert typeOf(pop3, SLOT_TYPE);
                        pop4 = pop();
                        assert typeOf(pop4, pop1);
                        push(pop3);
                        push(pop2);
                        push(pop1);
                    } else {
                        pop3 = pop();
                        assert typeOf(pop3, pop1);
                        push(pop2);
                        push(pop1);
                    }
                    return optimize(next);
                case Opcodes.DUP2:
                    pop1 = pop();
                    if (typeOf(pop1, SLOT_TYPE)) {
                        pop2 = pop();
                        assert typeOf(pop2, pop1);
                    }
                    return optimize(next);
                case Opcodes.DUP2_X1:
                    pop1 = pop();
                    pop2 = pop();
                    pop3 = pop();
                    if (typeOf(pop1, SLOT_TYPE) && typeOf(pop2, SLOT_TYPE) && typeOf(pop3, SLOT_TYPE)) {
                        pop4 = pop();
                        assert typeOf(pop4, pop1);
                        pop5 = pop();
                        assert typeOf(pop5, pop2);
                        push(pop3);
                        push(pop2);
                        push(pop1);
                    } else if (typeOf(pop1, DOUBLE_SLOT_TYPE) && typeOf(pop2, SLOT_TYPE)) {
                        assert typeOf(pop3, pop1);
                        push(pop2);
                        push(pop1);
                    } else {
                        throw new AssertionError("DUP2_X1 with error type:" + pop1.getDescriptor() + "," + pop2.getDescriptor() + "," + pop3.getDescriptor());
                    }
                    return optimize(next);
                case Opcodes.DUP2_X2:
                    pop1 = pop();
                    pop2 = pop();
                    pop3 = pop();
                    if (typeOf(pop1, DOUBLE_SLOT_TYPE) && typeOf(pop2, DOUBLE_SLOT_TYPE)) {
                        assert typeOf(pop3, pop1);
                        push(pop2);
                        push(pop1);
                    } else if (typeOf(pop1, SLOT_TYPE) && typeOf(pop2, SLOT_TYPE) && typeOf(pop3, DOUBLE_SLOT_TYPE)) {
                        pop4 = pop();
                        assert typeOf(pop4, pop1);
                        pop5 = pop();
                        assert typeOf(pop5, pop2);
                        push(pop3);
                        push(pop2);
                        push(pop1);
                    } else if (typeOf(pop1, DOUBLE_SLOT_TYPE) && typeOf(pop2, SLOT_TYPE) && typeOf(pop3, SLOT_TYPE)) {
                        pop4 = pop();
                        assert typeOf(pop4, pop1);
                        push(pop3);
                        push(pop2);
                        push(pop1);
                    } else if (typeOf(pop1, SLOT_TYPE) && typeOf(pop2, SLOT_TYPE) && typeOf(pop3, SLOT_TYPE) && typeOf(pop4 = pop(), SLOT_TYPE)) {
                        pop5 = pop();
                        assert typeOf(pop5, pop1);
                        pop6 = pop();
                        assert typeOf(pop6, pop2);
                        push(pop4);
                        push(pop3);
                        push(pop2);
                        push(pop1);
                    } else {
                        throw new AssertionError("DUP2_X2 with error type:" + pop1.getDescriptor() + "," + pop2.getDescriptor() + "," + pop3.getDescriptor());
                    }
                    return optimize(next);
                case Opcodes.SWAP:
                    pop1 = pop();
                    assert typeOf(pop1, SLOT_TYPE);
                    pop2 = pop();
                    assert typeOf(pop2, SLOT_TYPE);
                    push(pop1);
                    push(pop2);
                    return optimize(next);
                case Opcodes.IADD:
                case Opcodes.ISUB:
                case Opcodes.IMUL:
                case Opcodes.IDIV:
                case Opcodes.IREM:
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.IAND:
                case Opcodes.IOR:
                case Opcodes.IXOR:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.LADD:
                case Opcodes.LSUB:
                case Opcodes.LMUL:
                case Opcodes.LDIV:
                case Opcodes.LREM:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        push(Type.LONG_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.FADD:
                case Opcodes.FSUB:
                case Opcodes.FMUL:
                case Opcodes.FDIV:
                case Opcodes.FREM:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        push(Type.FLOAT_TYPE);
                        push(Type.FLOAT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.DADD:
                case Opcodes.DSUB:
                case Opcodes.DMUL:
                case Opcodes.DDIV:
                case Opcodes.DREM:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        push(Type.DOUBLE_TYPE);
                        push(Type.DOUBLE_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.INEG:
                case Opcodes.LNEG:
                case Opcodes.FNEG:
                case Opcodes.DNEG:
                    return optimize(next);
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LUSHR:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.IINC:
                    return optimize(next);
                case Opcodes.I2L:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.I2F:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.I2D:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.L2I:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.L2F:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.L2D:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.F2I:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.FLOAT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.F2L:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.FLOAT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.F2D:
                    if (typeOf(Type.DOUBLE_TYPE, type)) {
                        pop();
                        push(Type.FLOAT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a double value", type.getDescriptor()));
                    }
                case Opcodes.D2I:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.DOUBLE_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.D2L:
                    if (typeOf(Type.LONG_TYPE, type)) {
                        pop();
                        push(Type.DOUBLE_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a long value", type.getDescriptor()));
                    }
                case Opcodes.D2F:
                    if (typeOf(Type.FLOAT_TYPE, type)) {
                        pop();
                        push(Type.DOUBLE_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a float value", type.getDescriptor()));
                    }
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                    return optimize(next);
                case Opcodes.LCMP:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.LONG_TYPE);
                        push(Type.LONG_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.FCMPL:
                case Opcodes.FCMPG:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.FLOAT_TYPE);
                        push(Type.FLOAT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.DCMPL:
                case Opcodes.DCMPG:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(Type.DOUBLE_TYPE);
                        push(Type.DOUBLE_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.IFEQ:
                case Opcodes.IFNE:
                case Opcodes.IFLT:
                case Opcodes.IFGE:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                    return SKIP_INDEX_IF;
                case Opcodes.GOTO:
                    return SKIP_INDEX_GOTO;
                case Opcodes.JSR:
                case Opcodes.RET:
                    return SKIP_INDEX_JSR;
                case Opcodes.TABLESWITCH:
                case Opcodes.LOOKUPSWITCH:
                    return SKIP_INDEX_SWITCH;
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.ARETURN:
                case Opcodes.RETURN:
                    return SKIP_INDEX_RETURN;
                case Opcodes.GETSTATIC:
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                    Type fieldType = Type.getType(fieldInsnNode.desc);
                    if (typeOf(fieldType, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), fieldType.getDescriptor()));
                    }
                case Opcodes.PUTSTATIC:
                    fieldInsnNode = (FieldInsnNode) node;
                    push(Type.getType(fieldInsnNode.desc));
                    return optimize(next);
                case Opcodes.GETFIELD:
                    fieldInsnNode = (FieldInsnNode) node;
                    fieldType = Type.getType(fieldInsnNode.desc);
                    if (typeOf(fieldType, type)) {
                        pop();
                        push(Type.getObjectType(fieldInsnNode.owner));
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), fieldType.getDescriptor()));
                    }
                case Opcodes.PUTFIELD:
                    fieldInsnNode = (FieldInsnNode) node;
                    push(Type.getObjectType(fieldInsnNode.owner));
                    push(Type.getType(fieldInsnNode.desc));
                    return optimize(next);
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEINTERFACE:
                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                    Type returnType = Type.getReturnType(methodInsnNode.desc);
                    if (typeOf(returnType, Type.VOID_TYPE) || typeOf(returnType, type)) {
                        if (!typeOf(returnType, Type.VOID_TYPE)) {
                            pop();
                        }
                        if (methodInsnNode.getOpcode() != Opcodes.INVOKESTATIC) {
                            push(Type.getObjectType(methodInsnNode.owner));
                        }
                        Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                        for (Type argumentType : argumentTypes) {
                            push(argumentType);
                        }
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), returnType.getDescriptor()));
                    }
                case Opcodes.INVOKEDYNAMIC:
                    InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) node;
                    returnType = Type.getReturnType(invokeDynamicInsnNode.desc);
                    if (typeOf(returnType, Type.VOID_TYPE) || typeOf(returnType, type)) {
                        if (!typeOf(returnType, Type.VOID_TYPE)) {
                            pop();
                        }
                        Type[] argumentTypes = Type.getArgumentTypes(invokeDynamicInsnNode.desc);
                        for (Type argumentType : argumentTypes) {
                            push(argumentType);
                        }
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), returnType.getDescriptor()));
                    }
                case Opcodes.NEW:
                    TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                    Type newType = Type.getType("L" + typeInsnNode.desc + (typeInsnNode.desc.endsWith(";") ? "" : ";"));
                    if (typeOf(newType, type)) {
                        pop();
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), newType.getDescriptor()));
                    }
                case Opcodes.NEWARRAY:
                    IntInsnNode intInsnNode = (IntInsnNode) node;
                    Type newArrayType;
                    switch (intInsnNode.operand) {
                        case Opcodes.T_BOOLEAN:
                        case Opcodes.T_BYTE:
                        case Opcodes.T_CHAR:
                        case Opcodes.T_SHORT:
                        case Opcodes.T_INT:
                            newArrayType = Type.getType("[I");
                            break;
                        case Opcodes.T_FLOAT:
                            newArrayType = Type.getType("[F");
                            break;
                        case Opcodes.T_DOUBLE:
                            newArrayType = Type.getType("[D");
                            break;
                        case Opcodes.T_LONG:
                            newArrayType = Type.getType("[J");
                            break;
                        default:
                            throw new MethodCallOptException(String.format("can not NEWARRAY by  operand：%d", intInsnNode.operand));
                    }
                    if (typeOf(newArrayType, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), newArrayType.getDescriptor()));
                    }
                case Opcodes.ANEWARRAY:
                    typeInsnNode = (TypeInsnNode) node;
                    newType = Type.getType("[L" + typeInsnNode.desc + (typeInsnNode.desc.endsWith(";") ? "" : ";"));
                    if (typeOf(newType, type)) {
                        pop();
                        push(Type.INT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), newType.getDescriptor()));
                    }
                case Opcodes.ARRAYLENGTH:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(OBJECT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.ATHROW:
                    return SKIP_INDEX_ATHROW;
                case Opcodes.CHECKCAST:
                    typeInsnNode = (TypeInsnNode) node;
                    newType = Type.getType((typeInsnNode.desc.startsWith("[") ? "" : "L") + typeInsnNode.desc + (typeInsnNode.desc.endsWith(";") ? "" : ";"));
                    if (typeOf(newType, type)) {
                        pop();
                        //We can't clarify the spec¬ific type before here.so we use object
                        push(OBJECT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), newType.getDescriptor()));
                    }
                case Opcodes.INSTANCEOF:
                    if (typeOf(Type.INT_TYPE, type)) {
                        pop();
                        push(OBJECT_TYPE);
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a int value", type.getDescriptor()));
                    }
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                    push(OBJECT_TYPE);
                    return optimize(next);
//              case Opcodes.WIDE:
//                  break;
                case Opcodes.MULTIANEWARRAY:
                    MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) node;
                    Type multiANewArrayType = Type.getType(multiANewArrayInsnNode.desc);
                    if (typeOf(multiANewArrayType, type)) {
                        pop();
                        for (int i = 0; i < multiANewArrayInsnNode.dims; i++) {
                            push(Type.INT_TYPE);
                        }
                        return optimize(next);
                    } else {
                        throw new MethodCallOptException(String.format(OpcodesUtils.getOpcodeString(node.getOpcode()) + "(" + getLineNumber(next) + ")" + " There should be %s but find a %s value", type.getDescriptor(), multiANewArrayType.getDescriptor()));
                    }
                case Opcodes.IFNULL:
                case Opcodes.IFNONNULL:
//                    push(OBJECT_TYPE);
//                    return optimize(next);
                    return SKIP_INDEX_IF;
//              case Opcodes.GOTO_W:
//                  break;
//              case Opcodes.JSR_W:
//                  break;
                default:
                    throw new MethodCallOptException("unknow instruction:" + OpcodesUtils.covertToString(node));
            }
        }


        private String getStackString(final Stack<Type> paramStack) {
            StringBuilder stringBuilder = new StringBuilder().append("[");
            for (Type type : paramStack) {
                stringBuilder.append(type.getDescriptor()).append(";");
            }
            return stringBuilder.append("]").toString();
        }

        private int getLineNumber(int index) {
            AbstractInsnNode node = instructions.get(index);
            if (node instanceof LabelNode) {
                while (index < instructions.size()) {
                    node = instructions.get(index);
                    if (node instanceof LineNumberNode) {
                        return ((LineNumberNode) node).line;
                    }
                    index++;
                }
            } else {
                while (index >= 0) {
                    node = instructions.get(index);
                    if (node instanceof LineNumberNode) {
                        return ((LineNumberNode) node).line;
                    }
                    index--;
                }
            }
            return -1;
        }

    }

    /**
     * judge src instance of target<br/>
     *
     * @param src    src
     * @param target target
     * @return true if src instance of targe
     */
    private boolean typeOf(final Type src, final Type target) {
        if (src == target) {
            return true;
        }
        final int srcSort = src.getSort();
        final int targetSort = target.getSort();
        if (src == SLOT_TYPE) {
            if (target == DOUBLE_SLOT_TYPE) {
                return false;
            } else return
                    Type.BOOLEAN == targetSort ||
                            Type.BYTE == targetSort ||
                            Type.CHAR == targetSort ||
                            Type.SHORT == targetSort ||
                            Type.INT == targetSort ||
                            Type.FLOAT == targetSort ||
                            Type.OBJECT == targetSort ||
                            Type.ARRAY == targetSort;
        } else if (src == DOUBLE_SLOT_TYPE) {
            if (target == SLOT_TYPE) {
                return false;
            } else return
                    Type.DOUBLE == targetSort ||
                            Type.LONG == targetSort;
        } else if (target == SLOT_TYPE) {
            return
                    Type.BOOLEAN == srcSort ||
                            Type.BYTE == srcSort ||
                            Type.CHAR == srcSort ||
                            Type.SHORT == srcSort ||
                            Type.INT == srcSort ||
                            Type.FLOAT == srcSort ||
                            Type.OBJECT == srcSort ||
                            Type.ARRAY == srcSort;
        } else if (target == DOUBLE_SLOT_TYPE) {
            return Type.DOUBLE == srcSort ||
                    Type.LONG == srcSort;
        } else if ((srcSort == Type.BOOLEAN || srcSort == Type.BYTE || srcSort == Type.CHAR || srcSort == Type.SHORT || srcSort == Type.INT) &&
                (targetSort == Type.BOOLEAN || targetSort == Type.BYTE || targetSort == Type.CHAR || targetSort == Type.SHORT || targetSort == Type.INT)) {
            //all boolean、byte、char、short ->int
            return true;
        } else if (src == OBJECT_TYPE) {
            return targetSort == Type.OBJECT || targetSort == Type.ARRAY;
        } else if (target == OBJECT_TYPE) {
            return srcSort == Type.OBJECT || srcSort == Type.ARRAY;
        } else if (srcSort == Type.OBJECT && targetSort == Type.OBJECT) {
            //object   object
            return mContext.instanceofClass(src.getClassName(), target.getClassName());
        } else if (srcSort == Type.OBJECT && targetSort == Type.ARRAY) {
            //object  array
            return false;
        } else if (srcSort == Type.ARRAY && targetSort == Type.OBJECT) {
            //array  object
            //only java.lang.Object can receive an array object
            return OBJECT_TYPE.equals(target);
        } else if (srcSort == Type.ARRAY && targetSort == Type.ARRAY) {
            //array     array
            return typeOf(Type.getType(src.getDescriptor().substring(1)), Type.getType(target.getDescriptor().substring(1)));
        } else {
            //long、double、float
            return src.equals(target);
        }
    }
}
