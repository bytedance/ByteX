package com.ss.android.ugc.bytex.closeable.visitors;

import com.ss.android.ugc.bytex.closeable.CloseableCheckContext;
import com.ss.android.ugc.bytex.closeable.ControlFlowAnalyzer;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Triple;

/**
 * Class Analyzer<br/>
 * Analyze whether a closeable object exists in each method of a class<br/>
 */
public class CloseableCheckClassVisitor2 extends BaseClassVisitor {
    private CloseableCheckContext mContext;
    private String mClassName;
    private String mSimpleClassName;
    //top outer class
    private String mOutSimpleClassName;
    private boolean mNeedCheck;
    private List<String> mBadCloseableConditions = new ArrayList<>();

    public CloseableCheckClassVisitor2(CloseableCheckContext context) {
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
            super(Opcodes.ASM5, access, name, desc, signature, exceptions);
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
                        List<NotClosedInfo> notClosedInfos = checkCloseable();
                        Set<String> items = new HashSet<>();
                        for (NotClosedInfo notClosedInfo : notClosedInfos) {
                            if (!mContext.isEmptyCloseable(replaceDot2Slash(notClosedInfo.realType)) && !mContext.extension.getExcludeCloseableList().contains(replaceDot2Slash(notClosedInfo.realType))) {
                                String line = replaceSlash2Dot(mClassName) + "." + mMethodName +
                                        "(" + (mOutSimpleClassName == null ? mSimpleClassName : mOutSimpleClassName) + ".java:" + getLineNumber(notClosedInfo.index) + ");var " + notClosedInfo.name + "(" + notClosedInfo.realType + ")";
                                if (notClosedInfo.fromThrowException) {
                                    line += "-ExitInException";
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
                    mContext.getLogger().e(replaceSlash2Dot(mClassName) + "." + mMethodName + ":" + e.getMessage(), e);
                }
            } else {
                mContext.getLogger().w("null mv", String.format("%s.%s", replaceSlash2Dot(mClassName), mMethodName));
            }
        }

        /**
         * Check if analysis is required.<br/>
         * If there are no instructions related to Closeable in the method, we can skip the analysis
         */
        private boolean needCheckClose() {
            final int size = instructions.size();
            for (int i = 0; i < size; i++) {
                AbstractInsnNode node = instructions.get(i);
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                    Type returnType = Type.getReturnType(methodInsnNode.desc);
                    if (mContext.instanceofCloseable(replaceDot2Slash(returnType.getClassName()))) {
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
        private List<NotClosedInfo> checkCloseable() throws AnalyzerException {
            final List<NotClosedInfo> notClosed = new ArrayList<>();
            final Map<Integer, StructInstType> instTypeMap = new HashMap<>();
            final int size = instructions.size();
            resolveTryCatchHandlers();
            for (int index = 0; index < size; index++) {
                AbstractInsnNode insnNode = instructions.get(index);
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    final String methodDesc = methodInsnNode.desc;
                    Type returnType = Type.getReturnType(methodDesc);
                    if (("<init>".equals(methodInsnNode.name) && mContext.instanceofCloseable(methodInsnNode.owner)) || mContext.instanceofCloseable(replaceDot2Slash(returnType.getClassName()))) {
                        //new or return a closeable
                        Triple<String, String, Boolean> varInfo = getVarInfo(index + 1, true, false);
                        if (varInfo == null) {
                            continue;
                        }
                        String realType = "<init>".equals(methodInsnNode.name) ? methodInsnNode.owner : returnType.getClassName();
                        int type = "<init>".equals(methodInsnNode.name) ? StructInstType.TYPE_NEW : StructInstType.TYPE_METHOD_RETURN;
                        instTypeMap.put(index, new StructInstType(type, new StructCloseableInfo(index, varInfo.getFirst(), realType, varInfo.getSecond(), varInfo.getThird())));
                    } else if ("close".equals(methodInsnNode.name) && "()V".equals(methodInsnNode.desc) && mContext.instanceofCloseable(methodInsnNode.owner, false)) {
                        //close
                        Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, false);
                        if (varInfo == null) {
                            continue;
                        }
                        instTypeMap.put(index, new StructInstType(StructInstType.TYPE_CLOSE, new StructCloseableInfo(index, varInfo.getFirst(), varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird())));
                    } else if (Type.getArgumentTypes(methodInsnNode.desc).length == 1 &&
                            mContext.instanceofCloseable(replaceDot2Slash(Type.getArgumentTypes(methodInsnNode.desc)[0].getClassName()), false)) {
                        //invoke close method such as closeQuietly(Closeable closeable)
                        Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, false);
                        if (varInfo == null) {
                            continue;
                        }
                        instTypeMap.put(index, new StructInstType(StructInstType.TYPE_CLOSEABLE_PARAM_METHOD, new StructCloseableInfo(index, varInfo.getFirst(), varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird())));
                    } else {
                        //Suppose the function called here throws an exception.
                        List<String> methodExceptions = mContext.getExceptions(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
                        if (methodExceptions.size() > 0) {
                            instTypeMap.put(index, new StructInstType(StructInstType.TYPE_METHOD_EXCEPTION, new StructCloseableInfo(index, null, null, null, false, methodExceptions)));
                        }
                    }
                } else if (instructions.get(index).getOpcode() == Opcodes.ARETURN && mContext.instanceofCloseable(replaceDot2Slash(Type.getReturnType(mDesc).getClassName()), false)) {
                    //return a closeable object
                    Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, true);
                    if (varInfo == null) {
                        continue;
                    }
                    instTypeMap.put(index, new StructInstType(StructInstType.TYPE_RETURN_CLOSEABLE, new StructCloseableInfo(index, varInfo.getFirst(), varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird())));
                } else if (insnNode.getOpcode() == Opcodes.IFNULL) {
                    //If statement
                    Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, false);
                    if (varInfo != null) {
                        JumpInsnNode jumpInsnNode = (JumpInsnNode) insnNode;
                        int jumpIndex = instructions.indexOf(jumpInsnNode.label);
                        instTypeMap.put(jumpIndex, new StructInstType(StructInstType.TYPE_NULL, new StructCloseableInfo(index, varInfo.getFirst(), varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird())));
                    }
                } else if (insnNode.getOpcode() == Opcodes.IFNONNULL) {
                    //If statement
                    Triple<String, String, Boolean> varInfo = getVarInfo(index - 1, false, false);
                    if (varInfo != null) {
                        instTypeMap.put(index + 1, new StructInstType(StructInstType.TYPE_NON_NULL, new StructCloseableInfo(index, varInfo.getFirst(), varInfo.getFirst(), varInfo.getSecond(), varInfo.getThird())));
                    }
                }
            }


            ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer(mClassName, this);
            controlFlowAnalyzer.setFlowController(new ControlFlowAnalyzer.FlowController() {
                Map<Integer, Map<String, TryCatchBlockNode>> exceptionHandlers = new HashMap<>();

                @Override
                protected boolean newControlFlowExceptionEdge(final int insIndex, final TryCatchBlockNode tcb) {
                    if (!mContext.extension.isStrictMode()) {
                        return false;
                    }
                    Map<String, TryCatchBlockNode> stringTryCatchBlockNodeMap = exceptionHandlers.get(insIndex);
                    if (stringTryCatchBlockNodeMap == null) {
                        stringTryCatchBlockNodeMap = resolveExceptionHandler(insIndex);
                    }
                    return stringTryCatchBlockNodeMap.values().contains(tcb);
                }

                @Override
                protected int[] jump(List<Integer> current, int index, int[] next) {
                    if (instructions.get(index) instanceof MethodInsnNode) {
                        for (Integer integer : current) {
                            StructInstType structInstType = instTypeMap.get(integer);
                            if (structInstType != null && (structInstType.type == StructInstType.TYPE_METHOD_RETURN || structInstType.type == StructInstType.TYPE_NEW)) {
                                return next;
                            }
                        }
                        return new int[]{index + 1};
                    } else {
                        return next;
                    }
                }

                private Map<String, TryCatchBlockNode> resolveExceptionHandler(int index) {
                    List<String> exceptions = new ArrayList<>();
                    AbstractInsnNode insnNode = instructions.get(index);
                    if (insnNode.getOpcode() == Opcodes.ATHROW) {
                        AbstractInsnNode node = instructions.get(findNextValidOpcodeInsIndex(index - 1, true));
                        String athrowExceptionName = null;
                        if (node.getOpcode() == Opcodes.ALOAD) {
                            //throw e
                            LocalVariableNode variableNode = getLocalVariableNodeByIndex(((VarInsnNode) node).var);
                            if (variableNode != null) {
                                Type type = Type.getType(variableNode.desc);
                                if (type.getSort() == Type.OBJECT && mContext.instanceofClass(type.getInternalName(), "java/lang/Throwable")) {
                                    athrowExceptionName = variableNode.desc;
                                }
                            }
                        } else if (node.getOpcode() == Opcodes.INVOKESPECIAL) {
                            //throw new XXXException
                            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                            if ("<init>".equals(methodInsnNode.name) && mContext.instanceofClass(methodInsnNode.owner, "java/lang/Throwable")) {
                                athrowExceptionName = methodInsnNode.owner;
                            }
                        }
                        if (athrowExceptionName == null) {
                            athrowExceptionName = "java/lang/Throwable";
                        }
                        exceptions.add(athrowExceptionName);
                    } else {
                        StructInstType structInstType = instTypeMap.get(index);
                        if (structInstType != null && structInstType.type == StructInstType.TYPE_METHOD_EXCEPTION && structInstType.info.exceptions != null && !structInstType.info.exceptions.isEmpty()) {
                            exceptions.addAll(structInstType.info.exceptions);
                        }
                    }
                    if (exceptions.isEmpty()) {
                        return Collections.emptyMap();
                    }
                    List<TryCatchBlockNode> catchFinallyBlocks = catchFinallyHandlers[index];
                    if (catchFinallyBlocks == null) {
                        return Collections.emptyMap();
                    }
                    Map<String, TryCatchBlockNode> handlers = new HashMap<>();
                    for (String exception : exceptions) {
                        for (TryCatchBlockNode catchFinallyBlock : catchFinallyBlocks) {
                            if (catchFinallyBlock.type == null || mContext.instanceofClass(exception, catchFinallyBlock.type)) {
                                handlers.put(exception, catchFinallyBlock);
                                break;
                            }
                        }
                    }
                    return handlers;
                }
            });
            controlFlowAnalyzer.analyze().dispatch((routeIndex, route) -> {
                Map<String, StructCloseableInfo> openState = new HashMap<>();
                int routeSize = route.size();
                for (int i = 0; i < routeSize; i++) {
                    final int index = route.get(i);
                    boolean jumped = false;
                    if (i - 1 >= 0) {
                        final int last = route.get(i - 1);
                        jumped = Math.abs(index - last) > 1;
                    }

                    StructInstType structInstType = instTypeMap.get(index);
                    if (structInstType == null) {
                        continue;
                    }
                    switch (structInstType.type) {
                        case StructInstType.TYPE_NEW:
                        case StructInstType.TYPE_METHOD_RETURN:
                            if (mContext.extension.isIgnoreField() && structInstType.info.isField) {
                                continue;
                            }
                            String key = structInstType.info.type + "." + structInstType.info.name + "." + structInstType.info.isField;
                            StructCloseableInfo original = openState.put(key, structInstType.info);
                            if (original != null) {
                                notClosed.add(new NotClosedInfo(index, original.type, original.realType, original.name, original.isField));
                            }
                            break;
                        case StructInstType.TYPE_CLOSEABLE_PARAM_METHOD:
                        case StructInstType.TYPE_RETURN_CLOSEABLE:
                        case StructInstType.TYPE_NULL:
                        case StructInstType.TYPE_NON_NULL:
                        case StructInstType.TYPE_CLOSE:
                            if (StructInstType.TYPE_CLOSEABLE_PARAM_METHOD == structInstType.type && !mContext.extension.isIgnoreWhenMethodParam()) {
                                continue;
                            }
                            if (StructInstType.TYPE_RETURN_CLOSEABLE == structInstType.type && !mContext.extension.isIgnoreAsReturn()) {
                                continue;
                            }
                            if (StructInstType.TYPE_NULL == structInstType.type && !jumped) {
                                continue;
                            }
                            if (StructInstType.TYPE_NON_NULL == structInstType.type && jumped) {
                                continue;
                            }
                            key = structInstType.info.type + "." + structInstType.info.name + "." + structInstType.info.isField;
                            openState.remove(key);
                            break;
                        case StructInstType.TYPE_METHOD_EXCEPTION:
                            //Suppose the function called here throws an exception.
                            if (!mContext.extension.isIgnoreMethodThrowException() && openState.values().size() > 0) {
                                List<String> methodExceptions = structInstType.info.exceptions;
                                List<TryCatchBlockNode> finallyBlocks = getCatchFinallyBlocks(index, true);
                                if (finallyBlocks.size() > 0) {
                                    continue;
                                }
                                List<TryCatchBlockNode> catchBlockNodes = getCatchFinallyBlocks(index, false);
                                for (String methodException : methodExceptions) {
                                    if (catchBlockNodes.stream().noneMatch((it) -> it.type.equals(methodException) || mContext.instanceofClass(methodException, it.type))) {
                                        for (StructCloseableInfo value : openState.values()) {
                                            notClosed.add(new NotClosedInfo(value.index, value.type, value.realType, value.name, value.isField, true));
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
                for (StructCloseableInfo value : openState.values()) {
                    notClosed.add(new NotClosedInfo(value.index, value.type, value.realType, value.name, value.isField));
                }
            });
            return notClosed;
        }

        private Triple<String, String, Boolean> getVarInfo(int index, boolean store, boolean fromReturnAddress) {
            if (index >= instructions.size() || index < 0) {
                return null;
            }
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
                TryCatchBlockNode tryCatchBlock = tryCatchBlocks.get(i);
                int startIndex = instructions.indexOf(tryCatchBlock.start);
                int endIndex = instructions.indexOf(tryCatchBlock.end);
                for (int j = startIndex; j < endIndex; ++j) {
                    List<TryCatchBlockNode> insnHandlers = catchFinallyHandlers[j];
                    if (insnHandlers == null) {
                        insnHandlers = new ArrayList<>();
                        catchFinallyHandlers[j] = insnHandlers;
                    }
                    insnHandlers.add(tryCatchBlock);
                }
            }
        }

        private int findNextValidOpcodeInsIndex(int index, boolean reverse) {
            while (reverse ? index >= 0 : index < instructions.size()) {
                if (instructions.get(index).getOpcode() > 0) {
                    return index;
                }
                index += reverse ? -1 : 1;
            }
            return -1;
        }
    }

    private static String replaceDot2Slash(String str) {
        return str.replace('.', '/');
    }

    private static String replaceSlash2Dot(String str) {
        return str.replace('/', '.');
    }


    private static class StructInstType {
        static final int TYPE_METHOD_RETURN = 1;
        static final int TYPE_NEW = 2;
        static final int TYPE_NULL = 3;
        static final int TYPE_NON_NULL = 4;
        static final int TYPE_CLOSE = 5;
        static final int TYPE_CLOSEABLE_PARAM_METHOD = 6;
        static final int TYPE_METHOD_EXCEPTION = 7;
        static final int TYPE_RETURN_CLOSEABLE = 8;
        final int type;//0 non;1;method open;2 contruction open;4 close
        final StructCloseableInfo info;

        StructInstType(int type, StructCloseableInfo info) {
            this.type = type;
            this.info = info;
        }
    }

    private static class StructCloseableInfo {
        final int index;//open index
        final String type;//define type
        final String realType;//real type
        final String name;//var name
        final boolean isField;//is field
        final List<String> exceptions;

        StructCloseableInfo(int index, String type, String realType, String name, boolean isField) {
            this(index, type, realType, name, isField, null);
        }

        StructCloseableInfo(int index, String type, String realType, String name, boolean isField, List<String> exceptions) {
            this.index = index;
            this.type = type;
            this.realType = realType;
            this.name = name;
            this.isField = isField;
            this.exceptions = exceptions;
        }
    }

    private static class NotClosedInfo {
        final int index;
        final String type;
        final String realType;
        final String name;
        final boolean isField;
        final boolean fromThrowException;

        NotClosedInfo(int index, String type, String realType, String name, boolean isField) {
            this(index, type, realType, name, isField, false);
        }

        NotClosedInfo(int index, String type, String realType, String name, boolean isField, boolean fromThrowException) {
            this.index = index;
            this.type = type;
            this.realType = realType;
            this.name = name;
            this.isField = isField;
            this.fromThrowException = fromThrowException;
        }
    }
}
