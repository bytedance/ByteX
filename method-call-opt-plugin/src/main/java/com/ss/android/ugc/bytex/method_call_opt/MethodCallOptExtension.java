package com.ss.android.ugc.bytex.method_call_opt;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangzhiqian on 2019/3/16<br/>
 */
public class MethodCallOptExtension extends BaseExtension {
    private static final String SEPARATOR = "#";
    /**
     * Only the classes in the onlyCheckList will be optimized
     * Format:ClassName#MethodName#MethodDesc
     * Support pattern matching
     * For Example:
     * "com/ss*",
     * "com/bytedance*"
     */
    private List<String> onlyCheckList = new ArrayList<>();
    /**
     * The classes in the whiteList will skip check
     * Format:ClassName#FieldName#FieldDesc
     * Support pattern matching
     * such as "com/facebook/stetho*"
     */
    private List<String> whiteList = new ArrayList<>();
    /**
     * Descriptions of The no effect method
     * For examples:
     * "android/util/Log#v#(Ljava/lang/String;Ljava/lang/String;)I",
     * "android/util/Log#v#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
     * "android/util/Log#d#(Ljava/lang/String;Ljava/lang/String;)I",
     * "android/util/Log#d#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
     * "android/util/Log#i#(Ljava/lang/String;Ljava/lang/String;)I",
     * "android/util/Log#i#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
     * "android/util/Log#w#(Ljava/lang/String;Ljava/lang/String;)I",
     * "android/util/Log#w#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
     * "android/util/Log#e#(Ljava/lang/String;Ljava/lang/String;)I",
     * "android/util/Log#e#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
     * "android/util/Log#println#(ILjava/lang/String;Ljava/lang/String;)I",
     * <p>
     * "java/lang/Throwable#printStackTrace#()V",
     * "com/google/devtools/build/android/desugar/runtime/ThrowableExtension#printStackTrace#(Ljava/lang/Throwable;)V"
     */
    private List<String> methodList;
    /**
     * Whether to print all instructions of the optimized method
     */
    private boolean showAfterOptInsLog = false;

    public List<String> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<String> methodList) {
        this.methodList = methodList;
    }

    public boolean isShowAfterOptInsLog() {
        return showAfterOptInsLog;
    }

    public void setShowAfterOptInsLog(boolean showAfterOptInsLog) {
        this.showAfterOptInsLog = showAfterOptInsLog;
    }

    public List<String> getOnlyCheckList() {
        return onlyCheckList;
    }

    public void setOnlyCheckList(List<String> onlyCheckList) {
        this.onlyCheckList = onlyCheckList;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public String getName() {
        return "method_call_opt";
    }
}
