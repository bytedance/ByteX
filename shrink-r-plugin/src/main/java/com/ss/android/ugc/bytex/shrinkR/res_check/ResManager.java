package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.xml.XmlReader;
import com.ss.android.ugc.bytex.gradletoolkit.Artifact;
import com.ss.android.ugc.bytex.shrinkR.Context;
import com.ss.android.ugc.bytex.transformer.io.Files_;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ss.android.ugc.bytex.common.utils.Utils.resolveDollarChar;
import static com.ss.android.ugc.bytex.shrinkR.Context.getMatchByGroup;

public class ResManager {
    // key is resource type
    private final Map<String, Map<String, Resource>> resourceSet;
    // key is resource id
    private final Map<Integer, Resource> resourceMap;
    private final Context context;
    private final Map<String, Set<Pair<Pattern, Pattern>>> mWhiteList = new HashMap<>();
    private final Pattern keepListPattern = Pattern.compile("(?<package>([\\w]+\\.)*)R.(?<inner>[^.]+)(.(?<field>.+))?");
    private final ResourceCheckExtension extension;
    private final List<Pair<String, String>> referenceStore = new ArrayList<>();

    ResManager(Context context) {
        this.context = context;
        this.resourceMap = new HashMap<>();
        this.resourceSet = new HashMap<>();
        extension = context.resCheckExtension;
    }


    private void initWithWhiteList(List<String> whiteList) {
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

    void prepare() {
        collectResourceId();
        processAndroidManifest();
        resolve(context.getTransformContext().getArtifact(Artifact.RAW_RESOURCE_SETS));
        initWithWhiteList(extension.getKeepRes());
    }

    private void processAndroidManifest() {
        context.getTransformContext().getArtifact(Artifact.MERGED_MANIFESTS).stream()
                .filter(file -> file != null && file.isFile() && file.getName().endsWith(".xml"))
                .findFirst()
                .ifPresent(manifest -> {
                    XmlReader xmlReader = new XmlReader();
                    xmlReader.read(manifest, new RecordResReachXmlVisitor(context, manifest, null, null));
                });
    }

    void collectResourceId() {
        for (Map.Entry<String, Map<String, Object>> entry : context.getRFields().entrySet()) {
            String rClassName = entry.getKey();
            String resType = Utils.getInnerRClass(rClassName);
            Map<String, Object> fields = entry.getValue();
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                Object value = field.getValue();
                if (value instanceof Integer) {
                    Resource resource = new Resource(resType, field.getKey(), (Integer) value);
                    Map<String, Resource> map = resourceSet.computeIfAbsent(resType, e -> new HashMap<>());
                    if (map.containsKey(resource.getName())) {
                        resource = map.get(resource.getName());
                    } else {
                        map.put(resource.getName(), resource);
                        resourceMap.put(resource.getResId(), resource);
                    }
                    resource.addRClass(rClassName);
                }
            }
        }
        for (Pair<String, String> reference : referenceStore) {
            String type = reference.getFirst();
            String name = reference.getSecond();
            Map<String, Resource> resNameMap = resourceSet.get(type);
            if (resNameMap != null) {
                Resource resource = resNameMap.get(name);
                if (resource != null) {
                    resource.refer();
                }
            }
        }
        // release for gc
        referenceStore.clear();
    }

    public void reachResource(String fromResType, String fromResName, String toResType, String toResName) {
        Resource toRes = reachResource(toResType, toResName);
        if (fromResType != null && fromResName != null && toRes != null) {
            Resource fromRes = getResource(fromResType, fromResName);
            if (fromRes != null) {
                fromRes.reach(toRes);
            }
        }
    }

    public Resource reachResource(String type, String name) {
        Map<String, Resource> resNameMap = resourceSet.get(type);
        if (resNameMap != null) {
            Resource resource = resNameMap.get(name);
            if (resource != null) {
                resource.refer();
                return resource;
            }
        } else {
            referenceStore.add(Pair.of(type, name));
        }
        return null;
    }

    private Resource getResource(String type, String name) {
        Map<String, Resource> resNameMap = resourceSet.get(type);
        if (resNameMap != null) {
            return resNameMap.get(name);
        }
        return null;
    }

    public void reachResource(int id) {
        Resource resource = resourceMap.get(id);
        if (resource != null) {
            resource.refer();
        }
    }

    void defineResource(String type, String name, String path) {
        if (type.endsWith("-array")) {
            type = "array";
        } else if (type.equals("declare-styleable")) {
            type = "styleable";
        } else if (type.indexOf("-") > 0) {
            type = type.substring(0, type.indexOf("-"));
        }
        if ("style".equals(type)) {
            name = name.replaceAll("\\.", "_");
        }
        Map<String, Resource> resNameMap = resourceSet.computeIfAbsent(type, e -> new HashMap<>());
        Resource resource = resNameMap.get(name);
        if (resource != null) {
            resource.define(path);
        }
    }

    private Predicate<Resource> resourceShouldKeepFilter = resource -> {
        for (String rClass : resource.getrClass()) {
            if (shouldKeep(rClass, resource.getName())) {
                return false;
            }
        }
        if (resource.getAttributions().stream()
                .noneMatch(this::shouldCheck)) {
            return false;
        }
        return !resource.getAttributions().isEmpty();
    };

    public List<Resource> getAllUnReachResource() {
        if (!context.extension.isEnable() || !extension.isEnable()) {
            return Collections.emptyList();
        }
        Queue<Resource> resources = resourceMap.values().stream()
                .filter(resource -> !resource.canReach())
                .filter(resource -> !"id".equals(resource.getType()))
                .filter(resourceShouldKeepFilter)
                .collect(Collectors.toCollection(LinkedList::new));
        List<Resource> unReachResources = new LinkedList<>();
        while (!resources.isEmpty()) {
            Resource resource = resources.poll();
            unReachResources.add(resource);
            resources.addAll(
                    resource.getReferringResources().stream()
                            .filter(res -> res.decreaseReference() == 0)
                            .filter(resourceShouldKeepFilter)
                            .collect(Collectors.toList())
            );
        }
        return unReachResources;
    }

    private boolean shouldKeep(String className, String fieldName) {
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


    private boolean shouldCheck(String source) {
        for (String checkPath : extension.getOnlyCheck()) {
            if (source.contains(checkPath)) {
                return true;
            }
        }
        return extension.getOnlyCheck().isEmpty();
    }

    private void resolve(Collection<File> resDirs) {
        XmlReader xmlReader = new XmlReader();
        for (File res : resDirs) {
            if (!res.isDirectory()) {
                continue;
            }
            context.getLogger().d("FindResFolder", "res folder was founded: " + res.getAbsolutePath());
            for (File f : Files_.fileTreeTraverser().preOrderTraversal(res)) {
                if (f.isFile() && !f.getName().equalsIgnoreCase(".DS_Store")) {
                    Pair<String, String> resTypeAndName = getResTypeAndNameFromPath(f);
                    if (resTypeAndName != null) {
                        defineResource(resTypeAndName.getFirst(), resTypeAndName.getSecond(), f.getAbsolutePath());
                    }
                    if (f.getName().endsWith(".xml")) {
                        xmlReader.read(f, new RecordResReachXmlVisitor(context, f,
                                resTypeAndName != null ? resTypeAndName.getFirst() : null,
                                resTypeAndName != null ? resTypeAndName.getSecond() : null));
                    }
                }
            }
        }
    }


    private static Pair<String, String> getResTypeAndNameFromPath(File file) {
        String type = file.getParentFile().getName();
        String name = file.getName();
        int resNameEnd = name.indexOf(".");
        if (resNameEnd < 0) {
            return null;
        }
        return Pair.of(type, name.substring(0, resNameEnd));
    }
}
