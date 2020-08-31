package com.ss.android.ugc.bytex.refercheck;

import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.objectweb.asm.Type;

import kotlin.Pair;


public class InaccessibleNode {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_CLASS_NOT_FOUND = 1;
    public static final int TYPE_METHOD_NOT_FOUND = 2;
    public static final int TYPE_FIELD_NOT_FOUND = 3;
    public static final int TYPE_INACCESS = 4;
    public static final int TYPE_NOT_IMPLEMENT = 5;

    //调用点
    public String callClassName;
    public String callMethodName;
    public String callMethodDesc;
    public int callMethodAccess;
    public String callClassSourceFile;
    public int callLineNumber;
    //被调用方法
    public String memberClassName;
    public String memberName;
    public String memberDesc;
    public int memberAccess;

    public int type = TYPE_UNKNOWN;

    public InaccessibleNode(String callClassName, String callMethodName, String callMethodDesc, int callMethodAccess, String callClassSourceFile, int callLineNumber, String memberClassName, String memberName, String memberDesc, int memberAccess, int type) {
        this.callClassName = callClassName;
        this.callMethodName = callMethodName;
        this.callMethodDesc = callMethodDesc;
        this.callMethodAccess = callMethodAccess;
        this.callClassSourceFile = callClassSourceFile;
        this.callLineNumber = callLineNumber;
        this.memberClassName = memberClassName;
        this.memberName = memberName;
        this.memberDesc = memberDesc;
        this.memberAccess = memberAccess;
        this.type = type;
    }

    public String getType() {
        switch (type) {
            case TYPE_CLASS_NOT_FOUND:
                return "Class Not Found";
            case TYPE_METHOD_NOT_FOUND:
                return "Method Not Found";
            case TYPE_FIELD_NOT_FOUND:
                return "Field Not Found";
            case TYPE_INACCESS:
                return "Can Not Access";
            case TYPE_NOT_IMPLEMENT:
                return "Method Not Implement";
            default:
                return "Unknown";
        }
    }


    @Override
    public String toString() {
        StringBuilder r = new StringBuilder();
        r.append("[").append(getType()).append("] in ").append(callClassName).append("(").append(getSourceFile()).append(":").append(callLineNumber).append(")\n");
        r.append("\t").append(parseAccessDesc(callMethodAccess));
        Pair<String, String> pair = parseMemberDesc(callMethodName, callMethodDesc);
        r.append(pair.getFirst()).append(" ").append(pair.getSecond()).append("{\n");
        r.append("\t\t->").append(parseAccessDesc(memberAccess));
        pair = parseMemberDesc(memberName, memberDesc);
        r.append(pair.getFirst()).append(" ").append(Utils.replaceSlash2Dot(memberClassName)).append(".").append(pair.getSecond()).append("\n\t}");
        return r.toString();
    }

    public String getSourceFile() {
        if (callClassSourceFile == null) {
            return String.format("%s.java", Utils.resolveClassName(callClassName).getSecond());
        } else return callClassSourceFile;
    }


    private static String parseAccessDesc(int access) {
        if (access == 0) {
            return "";
        }
        StringBuilder r = new StringBuilder();
        if (TypeUtil.isPublic(access)) {
            r.append("public ");
        } else if (TypeUtil.isProtected(access)) {
            r.append("protected ");
        } else if (TypeUtil.isPrivate(access)) {
            r.append("private ");
        }
        if (TypeUtil.isStatic(access)) {
            r.append("static ");
        }
        return r.toString();
    }

    private static Pair<String, String> parseMemberDesc(String name, String desc) {
        Type type = Type.getType(desc);
        if (type.getSort() == Type.METHOD) {
            StringBuilder args = new StringBuilder();
            for (Type argumentType : type.getArgumentTypes()) {
                if (args.length() > 0) {
                    args.append(",");
                }
                args.append(argumentType.getClassName());
            }
            return new Pair<>(type.getReturnType().getClassName(),
                    name + "(" + args.toString() + ")");
        } else {
            return new Pair<>(type.getClassName(), name);
        }
    }
}