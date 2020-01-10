package com.ss.android.ugc.bytex.const_inline.reflect;

import com.ss.android.ugc.bytex.common.utils.Utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;


/**
 * Created by yangzhiqian on 2019/4/21<br/>
 * Desc:
 */
public class ReflectResolve {
    public static <T> T resolveLastLdc(int index, MethodNode node, Class<T> type) {
        if (index - 1 < 0 || index >= node.instructions.size()) {
            return null;
        }
        AbstractInsnNode abstractInsnNode = node.instructions.get(index - 1);
        if (abstractInsnNode instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
            if (ldcInsnNode.cst.getClass() == type) {
                return (T) ldcInsnNode.cst;
            }
        }
        return null;
    }

    public static String resolveLastLoadClass(int index, MethodNode node) {
        //*.class
        Type type = resolveLastLdc(index, node, Type.class);
        if (type != null) {
            return type.getInternalName();
        }
        AbstractInsnNode abstractInsnNode = node.instructions.get(index - 1);
        if (abstractInsnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
            if ("java/lang/Class".equals(methodInsnNode.owner) && "forName".equals(methodInsnNode.name)) {
                //Class.forName
                if ("(Ljava/lang/String;)Ljava/lang/Class;".equals(methodInsnNode.desc)) {
                    //Class.forName("")
                    String className = resolveLastLdc(index - 1, node, String.class);
                    if (className != null) {
                        return Utils.replaceDot2Slash(className);
                    } else {
                        return null;
                    }
                } else {
                    //public static Class<?> forName(String name, boolean initialize, ClassLoader loader)
                    return null;
                }
            } else if ("getClass".equals(methodInsnNode.name)) {
                //xxx.getClass();
//                return getLastLoadType(index - 1, node);
                //runtime type
                return null;
            }

        }
        return null;
    }

    public static String getLastLoadType(int index, MethodNode node) {
        if (index - 1 < 0 || index >= node.instructions.size()) {
            return null;
        }
        AbstractInsnNode abstractInsnNode = node.instructions.get(index - 1);
        if (abstractInsnNode.getOpcode() == Opcodes.ALOAD) {
            VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
            LocalVariableNode localVariableNodeByIndex = getLocalVariableNodeByIndex(index - 1, varInsnNode.var, node);
            if (localVariableNodeByIndex != null) {
                return Type.getType(localVariableNodeByIndex.desc).getInternalName();
            }
        } else if (abstractInsnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
            return Type.getType(methodInsnNode.desc).getReturnType().getInternalName();
        }
        return null;
    }

    private static LocalVariableNode getLocalVariableNodeByIndex(int insIndex, int index, MethodNode node) {
        if (node.localVariables == null) {
            return null;
        }
        for (Object localVariable : node.localVariables) {
            LocalVariableNode var = (LocalVariableNode) localVariable;
            if (var.index == index &&
                    node.instructions.indexOf(var.start) <= insIndex &&
                    node.instructions.indexOf(var.end) > insIndex) {
                return var;
            }
        }
        return null;
    }
}
