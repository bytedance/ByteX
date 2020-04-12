package com.ss.android.ugc.bytex.getter_setter_inline;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.ByteXExtension;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.MemberEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.getter_setter_inline.visitor.GetterOrSetterMethod;
import com.ss.android.ugc.bytex.getter_setter_inline.visitor.RefFieldEntity;
import com.ss.android.ugc.bytex.hookproguard.ClassInfo;
import com.ss.android.ugc.bytex.hookproguard.MethodInfo;
import com.ss.android.ugc.bytex.hookproguard.ProguardConfigurationAnalyzer;

import org.gradle.api.Project;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.ss.android.ugc.bytex.common.utils.Utils.convertToPatternString;
import static com.ss.android.ugc.bytex.common.utils.Utils.resolveDollarChar;

public final class Context extends BaseContext<GetterSettingInlineExtension> {
    private static final String SEPARATOR = "#";
    private final Map<String, GetterOrSetterMethod> gettersAndSetters = new ConcurrentHashMap<>(2 << 10);
    private final Map<String, RefFieldEntity> targetFields = new ConcurrentHashMap<>(512);
    private final Map<String, List<Pair<Pattern, Pattern>>> excludeClass = new HashMap<>(); // 白名单，keep住这些类的方法
    private final Set<String> keepAnnotationDescriptors = new HashSet<>();
    private ProguardConfigurationAnalyzer proguardConfigurationAnalyzer;

    Context(Project project, AppExtension android, GetterSettingInlineExtension extension) {
        super(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        proguardConfigurationAnalyzer.prepare(getTransformContext().getVariantName());
        initWithKeepList(extension.getKeepList());
        initKeepAnnotations();
    }

    private static String getKey(String owner, String name, String desc) {
        return owner + SEPARATOR + name + SEPARATOR + desc;
    }

    public void addGetterOrSetter(String className, String name, String desc, FieldInsnNode insnNode, MethodInfo methodInfo) {
        RefFieldEntity target = addTargetField(insnNode);
        gettersAndSetters.put(getKey(className, name, desc), new GetterOrSetterMethod(className, name, desc, target, insnNode, methodInfo));
        getLogger().d(String.format("Found %s method( owner = [%s], name = [%s], desc = [%s] ), target field( owner = [%s], name = [%s], desc = [%s] )",
                insnNode.getOpcode() == Opcodes.GETFIELD || insnNode.getOpcode() == Opcodes.GETSTATIC ? "getter" : "setter",
                className, name, desc,
                insnNode.owner, insnNode.name, insnNode.desc));
    }

    private RefFieldEntity addTargetField(FieldInsnNode fieldInsn) {
        RefFieldEntity target = new RefFieldEntity(new FieldEntity(MemberEntity.ACCESS_UNKNOWN, fieldInsn.owner, fieldInsn.name, fieldInsn.desc));
        RefFieldEntity existField = targetFields.putIfAbsent(getKey(fieldInsn.owner, fieldInsn.name, fieldInsn.desc), target);
        if (existField != null) {
            target = existField;
        }
        target.inc();
        return target;
    }

    public boolean isGetterOrSetterField(String owner, String name, String desc) {
        return targetFields.containsKey(getKey(owner, name, desc));
    }

    public boolean isGetterOrSetter(String owner, String name, String desc) {
        return gettersAndSetters.containsKey(getKey(owner, name, desc));
    }

    public FieldInsnNode getGetterOrSetterInlineInsn(String owner, String name, String desc) {
        GetterOrSetterMethod m = gettersAndSetters.get(getKey(owner, name, desc));
        if (m == null) {
            return null;
        }
        return m.getInsn();
    }

    public void prepare() {
        Graph graph = getClassGraph();
        Map<String, GetterOrSetterMethod> temp = new HashMap<>();
        for (Iterator<Map.Entry<String, GetterOrSetterMethod>> it = gettersAndSetters.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, GetterOrSetterMethod> entry = it.next();
            GetterOrSetterMethod method = entry.getValue();
            RefFieldEntity target = method.getTarget();
            try {
                if (isShouldSkipInline(graph, method, target)) {
                    throw new ShouldSkipInlineException("Method is override method.");
                }
                FieldEntity origin = target.origin();
                FieldEntity realField = graph.confirmOriginField(origin.className(), origin.name(), origin.desc());
                if (graph.get(realField.className()).entity.fromAndroid) {
                    throw new ShouldSkipInlineException("Target class is in android.jar.");
                } else {
                    if (!origin.equals(realField)) {
                        RefFieldEntity realRefField = new RefFieldEntity(realField);
                        realRefField.setCount(target.getCount());
                        method.setTarget(realRefField);
                        RefFieldEntity refFieldEntity = targetFields.get(getKey(realField.className(), realField.name(), realField.desc()));
                        if (refFieldEntity != null) {
                            refFieldEntity.setCount(refFieldEntity.getCount() + target.getCount());
                        } else {
                            targetFields.put(getKey(realField.className(), realField.name(), realField.desc()), realRefField);
                        }
                        target.setCount(0);
                        targetFields.remove(getKey(target.className(), target.name(), target.desc()));
                    }
                }
                if (proguardConfigurationAnalyzer.shouldKeep(getTransformContext().getVariantName(), graph, method.getMethodInfo())) {
                    throw new ShouldSkipInlineException("The class and method are kept by Proguard.");
                }
                graph.childrenOf(method.className())
                        .forEach(c -> temp.putIfAbsent(getKey(c.entity.name, method.name(), method.desc()), method));
            } catch (ShouldSkipInlineException e) {
                it.remove();
                target.dec();
                if (target.isFree()) {
                    targetFields.remove(getKey(target.className(), target.name(), target.desc()));
                }
                getLogger().d("ShouldSkipInline", String.format("Skip inline getter or setter method (owner = [%s], name = [%s], desc = [%s])",
                        method.className(), method.name(), method.desc()));
            }
        }
        gettersAndSetters.putAll(temp);
    }

    private boolean isShouldSkipInline(Graph graph, GetterOrSetterMethod method, RefFieldEntity target) {
        Node clazz = graph.get(target.className());
        if (clazz == null || clazz.entity.fromAndroid) {
            return true;
        }
        return graph.overrideFromSuper(method.className(), method.name(), method.desc()) || graph.overridedBySubclass(method.className(), method.name(), method.desc());
    }

    private void initWithKeepList(List<String> keepList) {
        if (!excludeClass.isEmpty()) {
            excludeClass.clear();
        }
        addKeepListEntry("j", Pair.of(Pattern.compile("java/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("o", Pair.of(Pattern.compile("org/apache/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("o", Pair.of(Pattern.compile("org/xml/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("o", Pair.of(Pattern.compile("org/w3c/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("o", Pair.of(Pattern.compile("org/json/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("o", Pair.of(Pattern.compile("org/xmlpull/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("j", Pair.of(Pattern.compile("javax/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("d", Pair.of(Pattern.compile("dalvik/.+"), Utils.PATTERN_MATCH_ALL));
        addKeepListEntry("[", Pair.of(Pattern.compile("(\\[L)+.+"), Utils.PATTERN_MATCH_ALL)); // 对象数组
        addKeepListEntry("[", Pair.of(Pattern.compile("\\[+[BCDFISZJ]"), Utils.PATTERN_MATCH_ALL)); // 数组
        if (keepList != null) {
            keepList.forEach(s -> {
                String[] split = s.split("#");
                String key = s.substring(0, 1);
                if (key.equals("*") || key.equals(".") || key.equals("?") || key.equals("+")) {
                    key = "";
                }
                if (split.length == 1) {
                    addKeepListEntry(key, Pair.of(Pattern.compile(convertToPatternString(resolveDollarChar(s))), Utils.PATTERN_MATCH_ALL));
                } else if (split.length == 2) {
                    addKeepListEntry(key, Pair.of(Pattern.compile(convertToPatternString(resolveDollarChar(split[0]))),
                            Pattern.compile(convertToPatternString(resolveDollarChar(split[1])))));
                }
            });
        }
//        excludeClass.forEach(clz -> Log.i(TAG, "Exclude checking class: " + clz));

//        if (!methodCache.isEmpty()) {
//            methodCache.clear();
//        }
    }

    private void initKeepAnnotations() {
        List<String> keepWithAnnotations = extension.getKeepWithAnnotations();
        if (keepWithAnnotations == null || keepWithAnnotations.isEmpty()) return;
        for (String annotation : keepWithAnnotations) {
            keepAnnotationDescriptors.add(TypeUtil.className2Desc(annotation));
        }
    }

    private void addKeepListEntry(String prefix, Pair<Pattern, Pattern> entry) {
        excludeClass.computeIfAbsent(prefix, k -> new ArrayList<>()).add(entry);
    }


    public boolean shouldKeep(String className) {
        return shouldKeep(className, ".*");
    }

    public boolean shouldKeep(String className, String methodName) {
        boolean matched = false;
        if (className.isEmpty()) {
            return false;
        }
        List<Pair<Pattern, Pattern>> keepList = getKeepList(className);
        if (keepList == null || keepList.isEmpty()) {
            return false;
        }
        for (Pair<Pattern, Pattern> pair : keepList) {
            Pattern classPat = pair.getFirst();
            Pattern methodPat = pair.getSecond();
            if (classPat.matcher(className).matches() && methodPat.matcher(methodName).matches()) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    private List<Pair<Pattern, Pattern>> getKeepList(String className) {
        List<Pair<Pattern, Pattern>> keepList = excludeClass.get(className.substring(0, 1));
        if (keepList == null || keepList.isEmpty()) {
            return excludeClass.get("");
        }
        return keepList;
    }

    public boolean isAnnotationToKeepGetterAndSetter(String descriptor) {
        return keepAnnotationDescriptors.contains(descriptor);
    }

    public void hookProguard(Project project) {
        if (project.getExtensions().findByType(ByteXExtension.class) == null) {
            proguardConfigurationAnalyzer = ProguardConfigurationAnalyzer.hook(project,
                    "transformClassesAndResourcesWith" + extension.getName().substring(0, 1).toUpperCase() + extension.getName().substring(1) + "For",
                    "transformClassesWith" + extension.getName().substring(0, 1).toUpperCase() + extension.getName().substring(1) + "For");
        } else {
            proguardConfigurationAnalyzer = ProguardConfigurationAnalyzer.hook(project);
        }
    }

    public boolean shouldKeep(ClassInfo classInfo) {
        return !shouldInlineMethodsInClass(classInfo.getName()) && proguardConfigurationAnalyzer.shouldKeep(getTransformContext().getVariantName(), classInfo);
    }

    public boolean shouldKeep(ClassInfo classInfo, MethodInfo methodInfo) {
        return proguardConfigurationAnalyzer.shouldKeep(getTransformContext().getVariantName(), classInfo, methodInfo);
    }

    private boolean shouldInlineMethodsInClass(String className) {
        for (String classNamePrefix : extension.getShouldInline()) {
            if (className.startsWith(classNamePrefix)) return true;
        }
        return false;
    }
}
