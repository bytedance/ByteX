package com.ss.android.ugc.bytex.method_call_opt;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by yangzhiqian on 2019/3/16<br/>
 */
public class MethodCallOptContext extends BaseContext<MethodCallOptExtension> {
    //method+des:[className...]
    private final Map<String, Set<String>> mOptimizationNeededMethods = new HashMap<>();
    private final List<Pattern> whiteListPattern = new ArrayList<>();
    private final List<Pattern> onlyCheckPattern = new ArrayList<>();

    MethodCallOptContext(Project project, AppExtension android, MethodCallOptExtension extension) {
        super(project, android, extension);
    }

    /**
     * Collect methods that need to be optimized
     */
    @Override
    public void init() {
        super.init();
        whiteListPattern.clear();
        onlyCheckPattern.clear();
        mOptimizationNeededMethods.clear();

        final List<String> whiteList = extension.getWhiteList();
        for (String item : whiteList) {
            if (item == null || "".equals(item.trim())) {
                //ignore empty item
                continue;
            }
            whiteListPattern.add(Pattern.compile(Utils.convertToPatternString(item)));
        }
        final List<String> onlyCheckList = extension.getOnlyCheckList();
        for (String item : onlyCheckList) {
            if (item == null || "".equals(item.trim())) {
                //ignore empty item
                continue;
            }
            onlyCheckPattern.add(Pattern.compile(Utils.convertToPatternString(item)));
        }

        List<String> methodList = extension.getMethodList();
        if (methodList != null) {
            String separator = extension.getSeparator();
            for (String item : methodList) {
                String[] split = item.split(separator);
                if (split.length != 3) {
                    getLogger().e("Method Error", "item must consist of 3 parts:" + item);
                    continue;
                }
                if ("".equals(split[0].trim())) {
                    getLogger().e("Method Error", "ClassName is empty!" + item);
                    continue;
                }
                if ("".equals(split[1].trim())) {
                    getLogger().e("Method Error", "MethodName is empty!" + item);
                    continue;
                }
                if ("".equals(split[2].trim())) {
                    getLogger().e("Method Error", "MethodDes is empty!" + item);
                    continue;
                }
                String key = getMethodUniqueKey("", split[1], split[2]);
                mOptimizationNeededMethods.computeIfAbsent(key, (it) -> new HashSet<>()).add(split[0].trim());
            }
        }
        getLogger().d("init", "Method:" + mOptimizationNeededMethods);
        getLogger().d("init", "onlyCheckList:" + onlyCheckPattern);
        getLogger().d("init", "whiteList:" + whiteListPattern);
    }

    public void release() {
        whiteListPattern.clear();
        onlyCheckPattern.clear();
        mOptimizationNeededMethods.clear();
    }

    public boolean isOptimizationNeededMethodsEmpty() {
        return mOptimizationNeededMethods.isEmpty();
    }

    public boolean isOptimizationNeededMethod(String className, String methodName, String desc, boolean isStatic) {
        String key = getMethodUniqueKey("", methodName, desc);
        if (mOptimizationNeededMethods.containsKey(key)) {
            Set<String> classes = mOptimizationNeededMethods.get(key);
            if (classes.contains(className)) {
                return true;
            }
            if (!isStatic) {
                boolean needCheckParent = false;
                Graph graph = getClassGraph();
                if (graph != null) {
                    Node node = graph.get(className);
                    if (node != null) {
                        for (MethodEntity method : node.entity.methods) {
                            if (method.name().equals(methodName) && method.desc().equals(desc)) {
                                needCheckParent = TypeUtil.isPublic(method.access()) || TypeUtil.isProtected(method.access());
                                break;
                            }
                        }
                    }
                }
                if (needCheckParent) {
                    for (String clazz : classes) {
                        if (instanceofClass(className, clazz)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean needCheck(String className, String methodName, String desc) {
        return needCheck(getMethodUniqueKey(className, methodName, desc));
    }

    public boolean needCheckClass(String className) {
        return this.needCheck(className);
    }

    public boolean instanceofClass(String child, String parent) {
        Graph graph = getClassGraph();
        return graph.inherit(Utils.replaceDot2Slash(child), Utils.replaceDot2Slash(parent)) || graph.implementOf(Utils.replaceDot2Slash(child), Utils.replaceDot2Slash(parent));
    }

    private boolean needCheck(String param) {
        if (isInWhiteList(param)) {
            return false;
        }
        if (onlyCheckPattern.isEmpty()) {
            return true;
        }
        for (Pattern pattern : onlyCheckPattern) {
            if (pattern.matcher(param).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInWhiteList(String param) {
        for (Pattern pattern : whiteListPattern) {
            if (pattern.matcher(param).matches()) {
                return true;
            }
        }
        return false;
    }

    private String getMethodUniqueKey(String className, String methodName, String desc) {
        String separator = extension.getSeparator();
        return Utils.replaceDot2Slash(className).trim() + separator + methodName.trim() + separator + desc.trim();
    }
}
