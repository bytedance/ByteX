package com.ss.android.ugc.bytex.refercheck;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.Bucket;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferenceLocation;

import org.gradle.api.Project;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReferCheckContext extends BaseContext<ReferCheckExtension> {
    // 不能存接口方法和抽象方法
    // key是ClassName#MethodName#descriptor
    private Bucket<Map<String, ReferenceLocation>> methodCache;
    private Bucket<Map<String, ReferenceLocation>> fieldCache;
    private final Map<String, ReferenceLocation> notAccessMethods = new ConcurrentHashMap<>();
    //    private final Map<String, MethodCallLocation> methodCache = new ConcurrentHashMap<>(2 << 18);
    public static final String SEPARATOR = "#";
    private WhiteList whiteList;

    public ReferCheckContext(Project project, AppExtension android, ReferCheckExtension extension) {
        super(project, android, extension);
    }

    public void prepare() {
        methodCache = new Bucket<>(new Map[58], ReferCheckContext::getCacheIndex);
        notAccessMethods.clear();
        for (int i = 0; i <= 25; i++) {
            methodCache.set(i, new ConcurrentHashMap<>());
        }
        for (int i = 32; i < 58; i++) {
            methodCache.set(i, new ConcurrentHashMap<>());
        }
        fieldCache = new Bucket<>(new Map[58], ReferCheckContext::getCacheIndex);
        for (int i = 0; i <= 25; i++) {
            fieldCache.set(i, new ConcurrentHashMap<>());
        }
        for (int i = 32; i < 58; i++) {
            fieldCache.set(i, new ConcurrentHashMap<>());
        }
        whiteList = new WhiteList();
        initWithWhiteList(extension.getWhiteList());
    }

    private void initWithWhiteList(List<String> whiteList) {
        this.whiteList.initWithWhiteList(whiteList);
        this.whiteList.addWhiteListEntry("a", Pair.of(Pattern.compile("android/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("java/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/apache/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xml/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/w3c/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/json/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xmlpull/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("c", Pair.of(Pattern.compile("com/android/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("javax/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("d", Pair.of(Pattern.compile("dalvik/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("(\\[L)+.+"), Utils.PATTERN_MATCH_ALL)); // 对象数组
        this.whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("\\[+[BCDFISZJ]"), Utils.PATTERN_MATCH_ALL)); // 数组
    }

    @Deprecated
    public void addMethod(String owner, String name, String desc) {
        String key = String.join(SEPARATOR, owner, name, desc);
        methodCache.get(owner).put(key, new ReferenceLocation(true));
    }

    @Deprecated
    public void addField(String owner, String name, String desc) {
        String key = String.join(SEPARATOR, owner, name, desc);
        fieldCache.get(owner).put(key, new ReferenceLocation(true));
    }

    private static int getCacheIndex(String owner) {
        char initial;
        if (owner.length() > 4 && owner.startsWith("com/")) {
            initial = owner.charAt(4);
            if (initial >= 'a' && initial <= 'z') {
                initial = (char) (initial - 32);
            }
        } else {
            initial = owner.charAt(0);
        }
        return (initial >= 'A' && initial <= 'Z') || (initial >= 'a' && initial <= 'z') ? initial - 'A' : 0;
    }

    public void addMethodIfNeed(int access, String ownerClz, String name, String desc, String clzLoc, String methodLoc, int line, String sourceFile) {
        String methodKey = String.join(SEPARATOR, ownerClz, name, desc);
        ReferenceLocation callLocation = new ReferenceLocation(false);
        callLocation.clzLoc = clzLoc;
        callLocation.methodLoc = methodLoc;
        callLocation.line = line;
        callLocation.sourceFile = sourceFile;
        callLocation.access = access;
        methodCache.get(ownerClz).putIfAbsent(methodKey, callLocation);
    }

    public void addFieldIfNeed(int access, String ownerClz, String name, String desc, String clzLoc, String methodLoc, int line, String sourceFile) {
        String methodKey = String.join(SEPARATOR, ownerClz, name, desc);
        ReferenceLocation callLocation = new ReferenceLocation(false);
        callLocation.clzLoc = clzLoc;
        callLocation.methodLoc = methodLoc;
        callLocation.line = line;
        callLocation.sourceFile = sourceFile;
        callLocation.access = access;
        fieldCache.get(ownerClz).putIfAbsent(methodKey, callLocation);
    }

    public void addNotAccessMethod(String ownerClz, String name, String desc, String clzLoc, String methodLoc, int line, String sourceFile) {
        String methodKey = String.join(SEPARATOR, ownerClz, name, desc);
        ReferenceLocation callLocation = new ReferenceLocation(false);
        callLocation.clzLoc = clzLoc;
        callLocation.methodLoc = methodLoc;
        callLocation.line = line;
        callLocation.sourceFile = sourceFile;
        notAccessMethods.put(methodKey, callLocation);
    }

    public boolean shouldCheck(String className) {
        return whiteList.shouldCheck(className);
    }

    public boolean shouldCheck(String className, String name) {
        return whiteList.shouldCheck(className, name);
    }

    public Map<String, ReferenceLocation> getNotFoundMethods() {
        Map<String, ReferenceLocation> notExistMethods = methodCache.asList().stream()
                .filter(Objects::nonNull)
                .map(Map::entrySet) // 提取每个Map的entrySet
                .flatMap(Collection::stream) // 提取entry
                .filter(entry -> !entry.getValue().isExist())
                .filter(entry -> {
                    String[] split = entry.getKey().split(SEPARATOR);
                    String className = split[0];
                    String methodName = split[1];
                    return (entry.getValue().inaccessible() || !checkIfSuperMethodExisted(getClassGraph(), className, split[1], split[2]))
                            && shouldCheck(className, methodName);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, ReferenceLocation> allNotFoundMethods = new HashMap<>(notAccessMethods.size() + notExistMethods.size());
        allNotFoundMethods.putAll(notAccessMethods);
        allNotFoundMethods.putAll(notExistMethods);
        return allNotFoundMethods;
    }

    private boolean checkIfSuperMethodExisted(Graph graph, String className, String methodName, String desc) {
        Node node = graph.get(className);
        if (node == null || node.parent == null) {
            return false;
        }
        MethodEntity originMethod = node.parent.confirmOriginMethod(methodName, desc);
        return originMethod != null && !TypeUtil.isAbstract(originMethod.access());
    }

    public Map<String, ReferenceLocation> getNotFoundFields() {
        return fieldCache.asList().stream()
                .filter(Objects::nonNull)
                .map(Map::entrySet) // 提取每个Map的entrySet
                .flatMap(Collection::stream) // 提取entry
                .filter(entry -> !entry.getValue().isExist())
                .filter(entry -> {
                    String[] split = entry.getKey().split(SEPARATOR);
                    String className = split[0];
                    String fieldName = split[1];
                    return shouldCheck(className, fieldName);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void releaseContext() {
        super.releaseContext();
        if (methodCache != null) {
            methodCache.release();
            methodCache = null;
        }
        if (fieldCache != null) {
            fieldCache.release();
            fieldCache = null;
        }
        notAccessMethods.clear();
    }

}
