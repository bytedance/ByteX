package com.ss.android.ugc.bytex.shrinkR;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.shrinkR.exception.NotFoundRField;
import com.ss.android.ugc.bytex.shrinkR.exception.RFieldNotFoundException;
import com.ss.android.ugc.bytex.shrinkR.res_check.AssetsCheckExtension;
import com.ss.android.ugc.bytex.shrinkR.res_check.Checker;
import com.ss.android.ugc.bytex.shrinkR.res_check.ResourceCheckExtension;
import com.ss.android.ugc.bytex.shrinkR.source.RFileWhiteList;

import org.gradle.api.Project;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ss.android.ugc.bytex.common.utils.Utils.resolveDollarChar;

public class Context extends BaseContext<ShrinkRExtension> {
    private final Set<String> shouldDiscardRClasses = ConcurrentHashMap.newKeySet(1000);
    // key is class name, value is R class static field.
    private final Map<String, Map<String, Object>> shouldBeInlinedRFields = new ConcurrentHashMap<>(3000);
    private final Map<String, Map<String, Object>> shouldSkipInlineRFields = new ConcurrentHashMap<>(3000);
    private final Map<String, Set<Pair<Pattern, Pattern>>> mWhiteList = new HashMap<>();
    private static final String PATTERN_KEEP_LIST = "(?<package>([\\w]+\\.)*)R.(?<inner>[^.]+)(.(?<field>.+))?";
    private final Pattern keepListPattern = Pattern.compile(PATTERN_KEEP_LIST);
    public ResourceCheckExtension resCheckExtension;
    public AssetsCheckExtension assetsCheckExtension;
    private Checker checker;
    private Set<NotFoundRField> notFoundRFields = ConcurrentHashMap.newKeySet();

    public Context(Project project, AppExtension android, ShrinkRExtension extension) {
        super(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        initWithWhiteList(extension.getKeepList());
    }

    private void initWithWhiteList(List<String> whiteList) {
        mWhiteList.clear();
        if (whiteList == null) {
            return;
        }
        for (String item : whiteList) {
            addWhiteList(item);
        }
    }

    private void addWhiteList(String item) {
        if (item.length() == 0) {
            return;
        }
        Matcher m = keepListPattern.matcher(item);
        if (!m.find()) {
            return;
        }
        String packageName = getMatchByGroup(m, "package");
        String className;
        String innerClass = getMatchByGroup(m, "inner");
        String fieldName = getMatchByGroup(m, "field");
        if (packageName != null && !packageName.isEmpty()) {
            packageName = packageName.replaceAll("\\.", "/");
        } else {
            packageName = "([\\w]+/)*";
        }
        if (innerClass != null && !innerClass.isEmpty()) {
            className = "R$" + innerClass;
        } else {
            className = "R";
        }
        if (fieldName != null && !fieldName.isEmpty()) {
            fieldName = Utils.convertToPatternString(fieldName);
        }


        if (fieldName == null || fieldName.isEmpty()) {
            mWhiteList.computeIfAbsent(innerClass != null ? innerClass : "", e -> new HashSet<>())
                    .add(Pair.of(Pattern.compile(resolveDollarChar(packageName + className)), Utils.PATTERN_MATCH_ALL));
        } else {
            mWhiteList.computeIfAbsent(innerClass != null ? innerClass : "", e -> new HashSet<>())
                    .add(Pair.of(Pattern.compile(resolveDollarChar(packageName + className)), Pattern.compile(resolveDollarChar(fieldName))));
        }
    }

    public Checker getChecker() {
        if (checker == null) {
            this.checker = new Checker(this);
        }
        return checker;
    }

    public boolean discardable(String relatviePath) {
        int end = relatviePath.lastIndexOf(".class");
        return end > 0 && shouldDiscardRClasses.contains(relatviePath.substring(0, end));
    }

    public void calculateDiscardableRClasses() {
        if (extension.isCompatRFileAssignInherit()) {
            //必须全部是可以被移除的
            shouldDiscardRClasses.removeAll(
                    shouldDiscardRClasses.stream().filter(
                            className -> {
                                Set<String> relativeClassNames = new HashSet<>();
                                ClassNode node = (ClassNode) getClassGraph().get(className);
                                getClassGraph().traverseAllChild(node, classNode -> relativeClassNames.add(classNode.entity.name));
                                while ((node = node.parent) != null) {
                                    if (!"java/lang/Object".equals(node.entity.name)) {
                                        relativeClassNames.add(node.entity.name);
                                    }
                                }
                                return !shouldDiscardRClasses.containsAll(relativeClassNames);
                            }

                    ).collect(Collectors.toSet())
            );
        }
    }

    public void addShouldDiscardRClasses(String className) {
        shouldDiscardRClasses.add(className);
    }

    public void addShouldBeInlinedRField(String owner, String name, Object value) {
        owner = getRealRClassName(owner);
        shouldBeInlinedRFields.computeIfAbsent(owner, k -> new ConcurrentHashMap<>(100)).put(name, value);
    }

    public void addSkipInlineRField(String owner, String name, Object value) {
        owner = getRealRClassName(owner);
        shouldSkipInlineRFields.computeIfAbsent(owner, k -> new ConcurrentHashMap<>(100)).put(name, value == null ? 0 : value);
    }

    public boolean shouldBeInlined(String owner, String name) {
        owner = getRealRClassName(owner);
        Map<String, Object> fields = shouldBeInlinedRFields.get(owner);
        return fields != null && !fields.isEmpty() && fields.containsKey(name);
    }

    public Object getRFieldValue(String owner, String name) {
        owner = getRealRClassName(owner);
        Map<String, Object> fields = shouldBeInlinedRFields.get(owner);
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        Object value = fields.get(name);
        if (value == null) {
            if (!shouldSkipInlineRFields.getOrDefault(owner, Collections.emptyMap()).containsKey(name)) {
                throw new RFieldNotFoundException();
            }
        }
        return value;
    }

    public boolean shouldKeep(String className, String fieldName) {
        boolean matched = false;
        className = getRealRClassName(className);
        Set<Pair<Pattern, Pattern>> whiteList = mWhiteList.get(Utils.getInnerRClass(className));
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        for (Pair<Pattern, Pattern> pair : whiteList) {
            Pattern classPat = pair.getFirst();
            Pattern fieldPat = pair.getSecond();
            if (fieldPat.matcher(fieldName).matches() && classPat.matcher(className).matches()) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    public void resolveResource() {
        getChecker().prepare();
    }

    public Map<String, Map<String, Object>> getRFields() {
        return shouldBeInlinedRFields;
    }

    public AppExtension android() {
        return android;
    }


    public Set<NotFoundRField> getNotFoundRFields() {
        return notFoundRFields;
    }

    public void addNotFoundRField(String className, String methodName, String owner, String name) {
        notFoundRFields.add(new NotFoundRField(className, methodName, owner, name));
    }

    public String getRealRClassName(String className) {
        if (!extension.isCompatRFileAssignInherit()) {
            return className;
        }
        return RFileWhiteList.Companion.getRealRClassName(className);
    }

    public static String getMatchByGroup(Matcher m, String name) {
        try {
            return m.group(name);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void releaseContext() {
        super.releaseContext();
        shouldDiscardRClasses.clear();
        shouldBeInlinedRFields.clear();
        shouldSkipInlineRFields.clear();
        mWhiteList.clear();
        checker = null;
        notFoundRFields.clear();
    }
}
