package com.ss.android.ugc.bytex.common.utils;

import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeUtil {
    private static Pattern paramsPat = Pattern.compile("(\\[?[BCZSIJFD])|(L[^;]+;)");

    public static Map<Integer, String> getCheckedAccessMapForMethod() {
        if (sAccessMapForMethod == null) {
            sAccessMapForMethod = getAccessMapForMethod();
        }
        return sAccessMapForMethod;
    }
    private static Map<Integer, String> sAccessMapForMethod;

    public static Map<Integer, String> getAccessMapForMethod() {
        HashMap<Integer, String> map = new HashMap<>(12);
        map.put(Opcodes.ACC_PUBLIC, "PUBLIC");
        map.put(Opcodes.ACC_PRIVATE,"PRIVATE");
        map.put(Opcodes.ACC_PROTECTED,"PROTECTED");
        map.put(Opcodes.ACC_STATIC,"STATIC");
        map.put(Opcodes.ACC_FINAL,"FINAL");
        map.put(Opcodes.ACC_SYNCHRONIZED, "SYNCHRONIZED");
        map.put(Opcodes.ACC_BRIDGE,"BRIDGE");
        map.put(Opcodes.ACC_VARARGS,"VARARGS");
        map.put(Opcodes.ACC_NATIVE,"NATIVE");
        map.put(Opcodes.ACC_ABSTRACT,"ABSTRACT");
        map.put(Opcodes.ACC_STRICT,"STRICT");
        map.put(Opcodes.ACC_SYNTHETIC,"SYNTHETIC");
        return map;
    }

    public static String removeFirstParam(String desc) {
        if (desc.startsWith("()")) {
            return desc;
        }
        int index = 1;
        char c = desc.charAt(index);
        while (c == '[') {
            index++;
            c = desc.charAt(index);
        }
        if (c == 'L') {
            while (desc.charAt(index) != ';') {
                index++;
            }
        }
        return "(" + desc.substring(index + 1);
    }

    public static int getParameterCountFromMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(') + 1;
        int endIndex = desc.lastIndexOf(')');
        String paramsDesc = desc.substring(beginIndex, endIndex);
        if (paramsDesc.isEmpty()) return 0;
        int count = 0;
        Matcher matcher = paramsPat.matcher(paramsDesc);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static List<String> splitMethodParametersFromDesc(String desc) {
        int beginIndex = desc.indexOf('(') + 1;
        int endIndex = desc.lastIndexOf(')');
        String paramsDesc = desc.substring(beginIndex, endIndex);
        if (paramsDesc.isEmpty()) return Collections.emptyList();
        List<String> types = new ArrayList<>();
        Matcher matcher = paramsPat.matcher(paramsDesc);
        while (matcher.find()) {
            types.add(matcher.group());
        }
        return types;
    }

    public static String getMethodReturnValue(String desc) {
        int startIndex = desc.lastIndexOf(')');
        return desc.substring(startIndex + 1);
    }

    public static String desc2Name(String desc) {
        if (!desc.startsWith("L") && !desc.endsWith(";")) {
            return desc;
        }
        return desc.substring(1, desc.length() - 1);
    }

    public static String className2Desc(String name) {
        if (name == null || name.isEmpty()) return null;
        return String.format("L%s;", name);
    }

    public static String descToStatic(int access, String desc, String className) {
        if ((access & Opcodes.ACC_STATIC) == 0) {
            desc = "(L" + className.replace('.', '/') + ";" + desc.substring(1);
        }
        return desc;
    }

    public static String descToNonStatic(String desc) {
        return "(" + desc.substring(desc.indexOf(';') + 1);
    }

    public static int parseArray(int index, String desc) {
        while (desc.charAt(index) == '[') index++;
        if (desc.charAt(index) == 'L') {
            while (desc.charAt(index) != ';') index++;
        }
        return index;
    }

    public static int parseObject(int index, String desc) {
        while (desc.charAt(index) != ';') index++;
        return index;
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }

    public static boolean isAbstract(int access) {
        return (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
    }

    public static boolean isNative(int access) {
        return (access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;
    }

    public static boolean isSynthetic(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC;
    }

    public static boolean isSynchronized(int access) {
        return (access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED;
    }

    public static boolean isPrivate(int access) {
        return (access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
    }

    public static boolean isPublic(int access) {
        return (access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC;
    }

    public static boolean isProtected(int access) {
        return (access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED;
    }

    public static boolean isPackage(int access) {
        return !isPrivate(access) && !isProtected(access) && !isPublic(access);
    }

    public static boolean isTransient(int access) {
        return (access & Opcodes.ACC_TRANSIENT) == Opcodes.ACC_TRANSIENT;
    }

    public static int resetAccessScope(int access, int scope) {
        return access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED) | scope;
    }

    public static boolean isInterface(int access) {
        return (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
    }

    public static boolean isInt(String desc) {
        return "I".equals(desc);
    }

    public static boolean isFinal(int access) {
        return (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
    }

    public static boolean isPrimitive(String desc) {
        return desc != null && desc.length() == 1 && "BCZSIJFD".contains(desc);
    }

    public static String getCanonicalName(String name) {
        return name.replace("/", ".");
    }

    public static String access2StringForMethod(int access) {
        return getCheckedAccessMapForMethod().get(access);
    }
}
