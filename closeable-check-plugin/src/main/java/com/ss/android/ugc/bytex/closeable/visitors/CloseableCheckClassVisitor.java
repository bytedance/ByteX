package com.ss.android.ugc.bytex.closeable.visitors;

import com.ss.android.ugc.bytex.closeable.CloseableCheckContext;
import com.ss.android.ugc.bytex.closeable.ControlFlowAnalyzer;
import com.ss.android.ugc.bytex.common.Constants;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Triple;


/**
 * Class Analyzer<br/>
 * Analyze whether a closeable object exists in each method of a class<br/>
 * see {@link CloseableCheckClassVisitor2}
 */
@Deprecated
public class CloseableCheckClassVisitor extends BaseClassVisitor {
    private CloseableCheckContext mContext;
    private String mClassName;
    private String mSimpleClassName;
    //top outer class
    private String mOutSimpleClassName;
    private boolean mNeedCheck;
    private List<String> mBadCloseableConditions = new ArrayList<>();

    public CloseableCheckClassVisitor(CloseableCheckContext context) {
        this.mContext = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        mSimpleClassName = mClassName.substring(mClassName.lastIndexOf("/") + 1);
        if (mSimpleClassName.contains("$")) {
            mOutSimpleClassName = mSimpleClassName.substring(0, mSimpleClassName.indexOf("$"));
        }
        mNeedCheck = mContext.needCheck(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (!mNeedCheck || mContext.inWhiteList(mClassName, name, desc)) {
            return methodVisitor;
        }
        return new CloseableCheckMethodVisitor(methodVisitor, access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (!mBadCloseableConditions.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String item : mBadCloseableConditions) {
                stringBuilder.append(item).append("\n");
            }
            mContext.getLogger().i("NotClosed", stringBuilder.toString());
        }
    }

    class CloseableCheckMethodVisitor extends MethodNode {
        private MethodVisitor mv;
        private String mMethodName;
        private String mDesc;

        CloseableCheckMethodVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
            super(Constants.ASM_API, access, name, desc, signature, exceptions);
            this.mv = mv;
            this.mMethodName = name;
            this.mDesc = desc;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (mv != null) {
                accept(mv);
                try {
                    if (needCheckClose()) {
                        List<CloseableOpenInfo> closeableOpenInfos = checkCloseable();
                        Set<String> items = new HashSet<>();
                        for (CloseableOpenInfo closeableOpenInfo : closeableOpenInfos) {
                            if (!closeableOpenInfo.hasSafeClosed() && !mContext.isEmptyCloseable(Utils.replaceDot2Slash(closeableOpenInfo.realType)) && !mContext.extension.getExcludeCloseableList().contains(Utils.replaceDot2Slash(closeableOpenInfo.realType))) {
                                String line = Utils.replaceSlash2Dot(mClassName) + "." + mMethodName +
                                        "(" + (mOutSimpleClassName == null ? mSimpleClassName : mOutSimpleClassName) + ".java:" + getLineNumber(closeableOpenInfo.index) + ");var " + closeableOpenInfo.name + "(" + closeableOpenInfo.realType + ")";
                                if (closeableOpenInfo.fromThrowException) {
                                    line += "-ExitInException";
                                }
                                if (closeableOpenInfo.hasSafeClosed(false)) {
                                    line += "-StrictMode";
                                }
                                items.add(line);
                            }
                        }
                        if (!items.isEmpty()) {
                            mBadCloseableConditions.add("------------" + mMethodName + "------------");
                            mBadCloseableConditions.addAll(items);
                        }
                    }
                } catch (AnalyzerException e) {
                    mContext.getLogger().e(Utils.replaceSlash2Dot(mClassName) + "." + mMethodName + ":" + e.getMessage(), e);
                }
            } else {
                mContext.getLogger().w("null mv", String.format("%s.%s", Utils.replaceSlash2Dot(mClassName), mMethodName));
            }

        }

        /**
         * Check if analysis is required.
         */
        private boolean needCheckClose() {
            final int size = instructions.size();
            for (int i = 0; i < size; i++) {
                AbstractInsnNode node = instructions.get(i);
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                    Type returnType = Type.getReturnType(methodInsnNode.desc);
                    if (mContext.instanceofCloseable(Utils.replaceDot2Slash(returnType.getClassName()))) {
                        // instruction that return a closeable
                        return true;
                    }
                } else if (node.getOpcode() == Opcodes.NEW) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                    if (mContext.instanceofCloseable(typeInsnNode.desc)) {
                        //new new an closeable object
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Check every possibility
         */
        private List<CloseableOpenInfo> checkCloseable() throws AnalyzerException {
            final Map<String, CloseableOpenInfo> closeables = new HashMap<>();
            final List<CloseableOpenInfo> closableConditions = new ArrayList<>();
            resolveTryCatchHandlers();
            ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer(mClassName, this);
            controlFlowAnalyzer.setFlowController(new ControlFlowAnalyzer.FlowController() {
                @Override
                public void onControlFlow(int index, AbstractInsnNode insNode) {
                    if (insNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insNode;
                        Type returnType = Type.getReturnType(methodInsnNode.desc);
                        if (instructions.size() > index + 1 &&
                                (mContext.instanceofCloseable(Utils.replaceDot2Slash(returnType.getClassName())) //return a closeable object from method call
                                        || (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(methodInsnNode.name) && mContext.instanceofCloseable(methodInsnNode.owner))//new a closeable object and call its constructor
                                )) {
                            //ASTORE for local var or PUTFIELD/PUTSTATIC for field expected
                            Triple<String, String, Boolean> varInfo = getVarInfo(index + 1, true, false);
                            if (varInfo == null || (varInfo.getThird() && mContext.extension.isIgnoreField())) {
                                return;
                            }
                            String realType = "<init>".equals(methodInsnNode.name) ? methodInsnNode.owner : returnType.getClassName();
                            List<TryCatchBlockNode> finallyBlocks = getCatchFinallyBlocks(index, true);
                            List<TryCatchBlockNode> catchBlocks = getCatchFinallyBlocks(index, false);
                            catchBlocks.sort((o1, o2) -> getLineNumber(instructions.indexOf(o1.handler)) - getLineNumber(instructions.indexOf(o2.handler)));
                            String key = getKey(varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird());
                            CloseableOpenInfo closeableOpenInfo = closeables.get(key);
                            if (closeableOpenInfo != null && index == closeableOpenInfo.index && closeableOpenInfo.hasSafeClosed(true)) {
                                //closed
                                return;
                            }
                            if (closeableOpenInfo != null) {
                                closableConditions.add(closeableOpenInfo);
                            }
                            closeables.put(key, new CloseableOpenInfo(
                                    index,
                                    varInfo.getFirst(),
                                    realType,
                                    varInfo.getSecond(),
                                    varInfo.getThird(),
                                    insNode,
                                    finallyBlocks,
                                    catchBlocks));
                        } else if (index - 1 >= 0 && "close".equals(methodInsnNode.name) && "()V".equals(methodInsnNode.desc) && mContext.instanceofCloseable(methodInsnNode.owner, false)) {
                            //invoke Closeable.close()
                            tryToRemoveCloseable(index, closeables);
                        } else if (index - 1 >= 0 && mContext.extension.isIgnoreWhenMethodParam() &&
                                Type.getArgumentTypes(methodInsnNode.desc).length == 1 &&
                                mContext.instanceofCloseable(Utils.replaceDot2Slash(Type.getArgumentTypes(methodInsnNode.desc)[0].getClassName()), false)) {
                            //invoke close method such as closeQuietly(Closeable closeable)
                            tryToRemoveCloseable(index, closeables);
                        } else {
                            //Suppose the function called here throws an exception.
                            if (!mContext.extension.isIgnoreMethodThrowException() && exceptions.size() > 0 && closeables.values().size() > 0) {
                                List<String> methodExceptions = mContext.getExceptions(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
                                if (!methodExceptions.isEmpty()) {
                                    for (CloseableOpenInfo value : closeables.values()) {
                                        if (value.hasSafeClosed(true) || value.finallyBlocks.size() > 0) {
                                            continue;
                                        }
                                        for (String methodException : methodExceptions) {
                                            if (value.exceptionBlocks.stream().noneMatch((it) -> it.type.equals(methodException) || mContext.instanceofClass(methodException, it.type))) {
                                                CloseableOpenInfo info = new CloseableOpenInfo(value);
                                                info.fromThrowException = true;
                                                closableConditions.add(info);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (mContext.extension.isIgnoreAsReturn() && instructions.size() > index + 1 && instructions.get(index + 1).getOpcode() == Opcodes.ARETURN && mContext.instanceofCloseable(Utils.replaceDot2Slash(Type.getReturnType(mDesc).getClassName()), false)) {
                        //return a closeable object
                        Triple<String, String, Boolean> varInfo = getVarInfo(index, false, true);
                        if (varInfo == null) {
                            return;
                        }
                        CloseableOpenInfo closeableOpenInfo = closeables.get(getKey(varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird()));
                        if (closeableOpenInfo != null) {
                            closeableOpenInfo.finallyClosed = true;
                            closeableOpenInfo.normalClosed = true;
                        }
                    }
                }

                @Override
                public boolean newControlFlowExceptionEdge(final int insIndex, final TryCatchBlockNode tcb) {
                    if (closeables.values().size() <= 0) {
                        return false;
                    }
                    AbstractInsnNode node = instructions.get(insIndex);
                    if (node.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                        return true;
                    }
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                        List<String> methodExceptions = mContext.getExceptions(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
                        return methodExceptions.size() > 0;
                    }
                    return false;
                }
            });
            controlFlowAnalyzer.analyze();
            closableConditions.addAll(closeables.values());
            return closableConditions;
        }

        private void tryToRemoveCloseable(int index, Map<String, CloseableOpenInfo> closeables) {
            Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, false);
            if (varInfo == null) {
                return;
            }
            final String key = getKey(varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird());
            CloseableOpenInfo openNode = closeables.get(key);
            if (openNode == null) {
                //close twice?
//                mContext.getLogger().w(String.format("%s.%s(LineNumber=%d)varName=%s:did you have opened a closeable object? or close method was called twice?", replaceSlash2Dot(mClassName), mMethodName, getLineNumber(index), varInfo.getSecond()));
                return;
            }
            for (TryCatchBlockNode finallyBlock : openNode.finallyBlocks) {
                if (getLineNumber(index) >= getLineNumber(instructions.indexOf(finallyBlock.handler))) {
                    //close in finally blocks
                    openNode.finallyClosed = true;
                    return;
                }
            }
            int size = openNode.exceptionBlocks.size();
            for (int i = 0; i < size; i++) {
                int start = getLineNumber(instructions.indexOf(openNode.exceptionBlocks.get(i).handler));
                int end = i + 1 >= size ? Integer.MAX_VALUE : getLineNumber(instructions.indexOf(openNode.exceptionBlocks.get(i + 1).handler));
                if (start <= getLineNumber(index) && getLineNumber(index) < end) {
                    //close in catch blocks
                    openNode.exceptionCloseState[i] = true;
                    return;
                }
            }
            //normal control flow
            openNode.normalClosed = true;
        }

        private Triple<String, String, Boolean> getVarInfo(int index, boolean store, boolean fromReturnAddress) {
            AbstractInsnNode node = instructions.get(index);
            String varType;
            String varName;
            boolean isField;
            if (node.getOpcode() == (store ? Opcodes.ASTORE : Opcodes.ALOAD)) {
                int var = ((VarInsnNode) node).var;
                LocalVariableNode localVariableNode = getLocalVariableNodeByIndex(var);
                if (localVariableNode == null || !localVariableNode.desc.startsWith("L")) {
                    if (fromReturnAddress) {
                        //ALOAD can not load a local var when load a return address
                        int size = instructions.size();
                        int lineNumber = getLineNumber(index);
                        for (int i = 0; i < size; i++) {
                            if (lineNumber == getLineNumber(i) &&
                                    instructions.get(i).getOpcode() == Opcodes.ALOAD &&
                                    i + 1 < size && instructions.get(i + 1).getOpcode() == Opcodes.ASTORE && ((VarInsnNode) instructions.get(i + 1)).var == var) {
                                //ALOAD ASTORE
                                localVariableNode = getLocalVariableNodeByIndex(((VarInsnNode) instructions.get(i)).var);
                                break;
                            }
                        }
                        if (localVariableNode == null) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
                varType = localVariableNode.desc.substring(1).replaceAll(";", "");
                varName = localVariableNode.name;
                isField = false;
            } else if (node.getOpcode() == (store ? Opcodes.PUTFIELD : Opcodes.GETFIELD)
                    || node.getOpcode() == (store ? Opcodes.PUTSTATIC : Opcodes.GETSTATIC)) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                varType = fieldInsnNode.owner;
                varName = fieldInsnNode.name;
                isField = true;
            } else {
                return null;
            }
            return new Triple<>(varType, varName, isField);
        }

        private LocalVariableNode getLocalVariableNodeByIndex(int index) {
            if (localVariables == null) {
                return null;
            }
            for (Object localVariable : localVariables) {
                if (localVariable instanceof LocalVariableNode && ((LocalVariableNode) localVariable).index == index) {
                    return (LocalVariableNode) localVariable;
                }
            }
            return null;
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

        private Map<Integer, List<TryCatchBlockNode>> tryCatchMaps = new HashMap<>();
        private Map<Integer, List<TryCatchBlockNode>> tryFinallyMaps = new HashMap<>();

        private List<TryCatchBlockNode> getCatchFinallyBlocks(int index, boolean isFinally) {
            if (!isFinally && tryCatchMaps.containsKey(index)) {
                return tryCatchMaps.get(index);
            }
            if (isFinally && tryFinallyMaps.containsKey(index)) {
                return tryFinallyMaps.get(index);
            }
            List<TryCatchBlockNode> finallyBlocks = new ArrayList<>();
            List<TryCatchBlockNode> catchFinallyBlocks = catchFinallyHandlers[index];
            if (catchFinallyBlocks != null) {
                for (TryCatchBlockNode catchFinallyBlock : catchFinallyBlocks) {
                    if ((isFinally == (catchFinallyBlock.type == null))) {
                        finallyBlocks.add(catchFinallyBlock);
                    }
                }
            }
            if (isFinally) {
                tryFinallyMaps.put(index, finallyBlocks);
            } else {
                tryCatchMaps.put(index, finallyBlocks);
            }
            return finallyBlocks;
        }


        private List<TryCatchBlockNode>[] catchFinallyHandlers;

        private void resolveTryCatchHandlers() {
            catchFinallyHandlers = (List<TryCatchBlockNode>[]) new List<?>[instructions.size()];
            for (int i = 0; i < tryCatchBlocks.size(); ++i) {
                TryCatchBlockNode tryCatchBlock = (TryCatchBlockNode) tryCatchBlocks.get(i);
                int startIndex = instructions.indexOf(tryCatchBlock.start);
                int endIndex = instructions.indexOf(tryCatchBlock.end);
                for (int j = startIndex; j < endIndex; ++j) {
                    List<TryCatchBlockNode> insnHandlers = catchFinallyHandlers[j];
                    if (insnHandlers == null) {
                        insnHandlers = new ArrayList<TryCatchBlockNode>();
                        catchFinallyHandlers[j] = insnHandlers;
                    }
                    insnHandlers.add(tryCatchBlock);
                }
            }
        }

        private String getKey(String type, String name, boolean isField) {
            return type + "-" + name + "-" + isField;
        }

        private class CloseableOpenInfo {
            final int index;
            final String type;
            final String realType;
            final String name;
            final boolean isField;
            final AbstractInsnNode node;
            final List<TryCatchBlockNode> finallyBlocks;
            final List<TryCatchBlockNode> exceptionBlocks;
            final boolean[] exceptionCloseState;
            boolean finallyClosed = false;
            boolean normalClosed = false;
            boolean fromThrowException = false;

            CloseableOpenInfo(int index, String type, String realType, String name, boolean isField, AbstractInsnNode node, List<TryCatchBlockNode> finallyBlocks, List<TryCatchBlockNode> exceptionBlocks) {
                this.index = index;
                this.type = type;
                this.realType = realType;
                this.name = name;
                this.isField = isField;
                this.node = node;
                this.finallyBlocks = finallyBlocks;
                this.exceptionBlocks = exceptionBlocks;
                this.exceptionCloseState = new boolean[exceptionBlocks.size()];
            }

            CloseableOpenInfo(CloseableOpenInfo info) {
                this.index = info.index;
                this.type = info.type;
                this.realType = info.realType;
                this.name = info.name;
                this.isField = info.isField;
                this.node = info.node;
                this.finallyBlocks = info.finallyBlocks;
                this.exceptionBlocks = info.exceptionBlocks;
                this.exceptionCloseState = new boolean[exceptionBlocks.size()];
                this.finallyClosed = info.finallyClosed;
                this.normalClosed = info.normalClosed;
                this.fromThrowException = info.fromThrowException;
                System.arraycopy(info.exceptionCloseState, 0, this.exceptionCloseState, 0, exceptionBlocks.size());
            }

            boolean allExceptionClosed() {
                for (boolean b : exceptionCloseState) {
                    if (!b) {
                        return false;
                    }
                }
                return true;
            }

            boolean hasSafeClosed() {
                return hasSafeClosed(mContext.extension.isStrictMode());
            }

            boolean hasSafeClosed(boolean isStrictMode) {
                if (isStrictMode) {
                    return finallyClosed || (allExceptionClosed() && normalClosed);
                } else {
                    return finallyClosed || normalClosed;
                }
            }
        }
    }
}
