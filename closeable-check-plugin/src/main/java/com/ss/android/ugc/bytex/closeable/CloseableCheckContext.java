package com.ss.android.ugc.bytex.closeable;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.gradle.api.Project;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import kotlin.Triple;

public class CloseableCheckContext extends BaseContext<CloseableCheckExtension> {
    private final List<Triple<Pattern, Pattern, Pattern>> whiteListPattern = new ArrayList<>();
    private final List<Pattern> onlyCheck = new ArrayList<>();
    private final Set<String> emptyCloseables = new HashSet<>();

    CloseableCheckContext(Project project, AppExtension android, CloseableCheckExtension extension) {
        super(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        whiteListPattern.clear();
        onlyCheck.clear();
        emptyCloseables.clear();
    }

    public void addEmptyCloseable(String className) {
        synchronized (emptyCloseables) {
            emptyCloseables.add(className);
        }
    }

    public boolean isEmptyCloseable(String className) {
        return emptyCloseables.contains(className);
    }

    public Set<String> getEmptyCloseables() {
        return emptyCloseables;
    }

    public void prepare() {
        whiteListPattern.clear();
        onlyCheck.clear();
        List<String> whiteList = extension.getWhiteList();
        final String separator = extension.getSeparator();
        for (String item : whiteList) {
            if (item == null || "".equals(item.trim())) {
                //ignore empty item
                continue;
            }
            String[] splits = item.split(separator);
            if (splits.length < 1) {
                continue;
            }
            Pattern classPattern = Pattern.compile(Utils.convertToPatternString(splits[0]));
            Pattern methodPattern = Utils.PATTERN_MATCH_ALL;
            Pattern descPattern = Utils.PATTERN_MATCH_ALL;
            if (splits.length > 1) {
                methodPattern = Pattern.compile(Utils.convertToPatternString(splits[1]));
            }
            if (splits.length > 2) {
                descPattern = Pattern.compile(Utils.convertToPatternString(splits[2]));
            }
            whiteListPattern.add(new Triple<>(classPattern, methodPattern, descPattern));
        }

        List<String> onlyCheckList = extension.getOnlyCheckList();
        for (String item : onlyCheckList) {
            if (item == null || "".equals(item.trim())) {
                //ignore empty item
                continue;
            }
            onlyCheck.add(Pattern.compile(Utils.convertToPatternString(item)));
        }

        Iterator<String> iterator = emptyCloseables.iterator();
        Graph graph = getClassGraph();
        MethodEntity methodEntity = new MethodEntity(Opcodes.ACC_PUBLIC, "", "close", "()V");
        while (iterator.hasNext()) {
            methodEntity.setClassName(iterator.next());
            if (graph.overridedBySubclass(methodEntity.className(), methodEntity.name(), methodEntity.desc())) {
                iterator.remove();
                getLogger().d("remove empty closeable", methodEntity.className());
            }
        }
    }

    public boolean instanceofCloseable(String desc) {
        return instanceofCloseable(desc, true);
    }

    public boolean instanceofCloseable(String desc, boolean useCheckList) {
        if (instanceofClass(desc, "java/io/Closeable")) {
            if (!useCheckList) {
                return true;
            }
            List<String> closeableList = extension.getCloseableList();
            for (String closable : closeableList) {
                if (instanceofClass(desc, closable)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean instanceofClass(String className, String targetClassName) {
        try {
            return getClassGraph().instanceofClass(className, targetClassName);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean needCheck(String className) {
        if (onlyCheck.isEmpty()) {
            return true;
        }
        for (Pattern pattern : onlyCheck) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean inWhiteList(String className, String methodName, String desc) {
        for (Triple<Pattern, Pattern, Pattern> patterns : whiteListPattern) {
            if (patterns.getFirst().matcher(className).matches()
                    && patterns.getSecond().matcher(methodName).matches()
                    && patterns.getThird().matcher(desc).matches()) {
                return true;
            }
        }
        return false;
    }

    public List<String> getExceptions(String className, String methodName, String desc) {
        MethodEntity methodEntity = getMethodEntity(className, methodName, desc);
        if (methodEntity == null) {
            getLogger().w("method not found", String.format("method %s.%s.%s not found!", className, methodName, desc));
            return Collections.emptyList();
        }
        return methodEntity.exceptions;
    }

    private MethodEntity getMethodEntity(String className, String methodName, String des) {
        Node node = getClassGraph().get(className);
        if (node == null) {
            getLogger().w("class not found", String.format("class %s not found!", className));
            return null;
        }
        for (MethodEntity method : node.entity.methods) {
            if (method.name().equals(methodName) && method.desc().equals(des)) {
                return method;
            }
        }
        final Queue<Node> interfaces = new LinkedList<>();
        if (node.interfaces != null) {
            interfaces.addAll(node.interfaces);
        }
        Node parent = node.parent;
        while (parent != null) {
            for (MethodEntity method : parent.entity.methods) {
                if (method.name().equals(methodName) && method.desc().equals(des)) {
                    return method;
                }
            }
            if (parent.interfaces != null) {
                interfaces.addAll(parent.interfaces);
            }
            parent = parent.parent;
        }

        while (!interfaces.isEmpty()) {
            Node aInterface = interfaces.poll();
            for (MethodEntity method : aInterface.entity.methods) {
                if (method.name().equals(methodName) && method.desc().equals(des)) {
                    return method;
                }
            }
            if (aInterface.interfaces != null) {
                interfaces.addAll(aInterface.interfaces);
            }
        }
        return null;
    }

    public final void releaseContext() {
        super.releaseContext();
        whiteListPattern.clear();
        onlyCheck.clear();
        emptyCloseables.clear();
    }
}
