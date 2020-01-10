package com.ss.android.ugc.bytex.shrinkR;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.shrinkR.exception.NotFoundRField;
import com.ss.android.ugc.bytex.shrinkR.exception.RFieldNotFoundException;
import com.ss.android.ugc.bytex.shrinkR.res_check.AssetsCheckExtension;
import com.ss.android.ugc.bytex.shrinkR.res_check.Checker;
import com.ss.android.ugc.bytex.shrinkR.res_check.ResourceCheckExtension;

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

import static com.ss.android.ugc.bytex.common.utils.Utils.resolveDollarChar;

public class Context extends BaseContext<ShrinkRExtension> {
    private final Set<String> RClasses = ConcurrentHashMap.newKeySet(1000);
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

    public Checker getChecker() {
        if (checker == null) {
            this.checker = new Checker(this);
        }
        return checker;
    }
    //    private static final Pattern rClassNamePattern = Pattern.compile("([\\w]+\\.)+R(\\$.+)?");
    //    private static final Pattern rClassPattern = Pattern.compile("([\\w]+/)*R(\\$.+)?");
//    private static final Pattern rFilePattern = Pattern.compile("([\\w]+/)*R(\\$.+)?\\.class");


    public Context(Project project, AppExtension android, ShrinkRExtension extension) {
        super(project, android, extension);
    }

    public boolean discardable(String relatviePath) {
        int end = relatviePath.lastIndexOf(".class");
        return end > 0 && RClasses.contains(relatviePath.substring(0, end));
    }

    public void addRClass(String className) {
        RClasses.add(className);
    }

    public void addRField(String owner, String name, Object value) {
        shouldBeInlinedRFields.computeIfAbsent(owner, k -> new HashMap<>(100)).put(name, value);
    }

    public void addSkipRField(String owner, String name, Object value) {
        shouldSkipInlineRFields.computeIfAbsent(owner, k -> new HashMap<>(100)).put(name, value);
    }

    public boolean containRField(String owner, String name) {
        Map<String, Object> fields = shouldBeInlinedRFields.get(owner);
        return fields != null && !fields.isEmpty() && fields.containsKey(name);
    }

    public Object getRFieldValue(String owner, String name) {
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

    public void initWithWhiteList(List<String> whiteList) {
        if (whiteList == null) {
            return;
        }
        for (String item : whiteList) {
            addWhiteList(item);
        }
    }

    public static String getMatchByGroup(Matcher m, String name) {
        try {
            return m.group(name);
        } catch (Exception e) {
            return "";
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

    public boolean shouldKeep(String className, String fieldName) {
        boolean matched = false;
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
}
