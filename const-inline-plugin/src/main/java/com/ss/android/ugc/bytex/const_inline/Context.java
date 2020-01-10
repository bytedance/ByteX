package com.ss.android.ugc.bytex.const_inline;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.const_inline.reflect.model.ReflectFieldModel;

import org.gradle.api.Project;
import org.objectweb.asm.tree.FieldNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Context extends BaseContext<ConstInlineExtension> {
    private static final String DESC_ALL = "";
    private final List<Pattern> whiteListPatterns = new LinkedList<>();
    private final Set<String> skipWithAnnotations = new HashSet<>();
    private final Map<String, FieldNode> constFieldNodes = new HashMap<>(100000);
    private final Set<String> runtimeConstFields = new HashSet<>(100);
    private final Set<String> skipAnnotationClasses = new HashSet<>(100);
    private final Set<String> stringPool = new HashSet<>();
    private final Set<ReflectFieldModel> reflectFieldModels = new HashSet<>();


    private final Set<String> unknownClassNameReflectFieldNames = new HashSet<>();
    //getField(fieldName)
    private final Set<ReflectFieldModel> getFieldReflectModels = new HashSet<>();
    //getFields()
    private final Set<String> getFieldsReflectClassNames = new HashSet<>();//className
    //getDeclaredField(fieldName)
    private final Set<String> getDeclaredFieldReflectFields = new HashSet<>();//className+fieldName
    //getDeclaredFields()
    private final Set<String> getDeclaredFieldsReflectClassNames = new HashSet<>();//className


    Context(Project project, AppExtension android, ConstInlineExtension extension) {
        super(project, android, extension);
    }

    public void addConstField(String className, int access, String name, String desc, String signature, Object value) {
        if ("serialVersionUID".equals(name)) {
            //filter
            return;
        }
        final String key = getKey(className, name, desc);
        synchronized (constFieldNodes) {
            if (constFieldNodes.containsKey(key)) {
                throw new ConstInlineException("you have duplicate class with same class：" + className + ".please remove duplicate classes");
            }
            constFieldNodes.put(key, new FieldNode(access, name, desc, signature, value));
        }
    }

    public FieldNode getConstField(String className, String name, String desc, boolean safe) {
        String key = getKey(className, name, desc);
        if (safe) {
            return constFieldNodes.get(key);
        } else {
            synchronized (constFieldNodes) {
                return constFieldNodes.get(key);
            }
        }
    }

    public void addRuntimeConstField(String className, String name, String desc) {
        final String field = getKey(className, name, desc);
        synchronized (runtimeConstFields) {
            if (runtimeConstFields.contains(field)) {
                getLogger().d(field + "has already been assigned before!");
            }
            runtimeConstFields.add(field);
        }
    }

    public void addReflectClassConstField(ReflectFieldModel reflectFieldModel) {
        synchronized (reflectFieldModels) {
            reflectFieldModels.add(reflectFieldModel);
        }
    }

    public void addString(String str) {
        synchronized (stringPool) {
            stringPool.add(str);
        }
    }

    /**
     * ignore thread safety
     */
    public boolean inStringPool(String str) {
        return stringPool.contains(str);
    }


    public void addSkipAnnotationClass(String className) {
        synchronized (skipAnnotationClasses) {
            skipAnnotationClasses.add(className);
        }
    }

    /**
     * ignore thread safety
     */
    public boolean inSkipAnnotationClass(String className) {
        return skipAnnotationClasses.contains(className);
    }

    @Override
    public void init() {
        super.init();
        constFieldNodes.clear();
        runtimeConstFields.clear();
        reflectFieldModels.clear();
        stringPool.clear();
        unknownClassNameReflectFieldNames.clear();
        getFieldReflectModels.clear();
        getFieldsReflectClassNames.clear();
        getDeclaredFieldReflectFields.clear();
        getDeclaredFieldsReflectClassNames.clear();
        whiteListPatterns.clear();
        skipWithAnnotations.clear();
        skipAnnotationClasses.clear();
        for (String whiteList : extension.getWhiteList()) {
            whiteListPatterns.add(Pattern.compile(Utils.convertToPatternString(Utils.replaceSlash2Dot(whiteList))));
        }
        for (String annotation : extension.getSkipWithAnnotations()) {
            skipWithAnnotations.add("L" + Utils.replaceDot2Slash(annotation) + ";");
        }
    }

    public void prepare() {
        for (ReflectFieldModel reflectFieldModel : reflectFieldModels) {
            if (reflectFieldModel.owner == null) {
                if (reflectFieldModel.memberName != null) {
                    unknownClassNameReflectFieldNames.add(reflectFieldModel.memberName);
                }
            } else if (reflectFieldModel.memberName == null) {
                if (reflectFieldModel.isDeclared) {
                    //getDeclaredFields
                    getDeclaredFieldsReflectClassNames.add(reflectFieldModel.owner);
                } else {
                    //getFields
                    getFieldsReflectClassNames.add(reflectFieldModel.owner);
                }
            } else {
                if (!reflectFieldModel.isDeclared) {
                    //getField
                    getFieldReflectModels.add(reflectFieldModel);
                } else {
                    //getDeclaredField
                    getDeclaredFieldReflectFields.add(getKey(reflectFieldModel.owner, reflectFieldModel.memberName, DESC_ALL));
                }
            }
        }
    }

    public boolean isRuntimeConstField(String className, String name, String desc) {
        return runtimeConstFields.contains(getKey(className, name, desc));
    }

    public boolean isReflectField(int access, String className, String name) {
        if (unknownClassNameReflectFieldNames.contains(name)) {
            return true;
        }
        //getDeclaredField
        if (getDeclaredFieldReflectFields.contains(getKey(className, name, DESC_ALL))) {
            return true;
        }
        //getDeclaredFields
        if (getDeclaredFieldsReflectClassNames.contains(className)) {
            return true;
        }
        if (TypeUtil.isPublic(access)) {
            Set<String> classNames = new HashSet<>();
            //getField
            for (ReflectFieldModel reflectFieldModel : getFieldReflectModels) {
                if (name.equals(reflectFieldModel.memberName)) {
                    classNames.clear();
                    getAllSuperAndChildClass(reflectFieldModel.owner, classNames, true);
                    if (classNames.contains(className)) {
                        return true;
                    }
                }
            }

            //getFields
            if (getFieldsReflectClassNames.contains(className)) {
                //fast check
                return true;
            }

            for (String reflectClassName : getFieldsReflectClassNames) {
                classNames.clear();
                getAllSuperAndChildClass(reflectClassName, classNames, true);
                if (classNames.contains(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inWhiteList(String className, String name, String desc) {
        String key = getKey(className, name, desc);
        for (Pattern whiteListPattern : whiteListPatterns) {
            if (whiteListPattern.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean inSkipWithAnnotations(String desc) {
        return skipWithAnnotations.contains(desc);
    }

    private static String getKey(String className, String name, String desc) {
        return Utils.replaceSlash2Dot(className) + "." + name + "." + desc;
    }


    private void getAllSuperAndChildClass(String classClass, final Set<String> classNames, boolean travelChildren) {
        if (classNames.contains(classClass)) {
            return;
        }
        Graph classGraph = getClassGraph();
        if (classGraph == null) {
            return;
        }
        Node node = classGraph.get(classClass);
        if (node == null) {
            return;
        }
        classNames.add(node.entity.name);
        if (node instanceof ClassNode) {
            //class
            ClassNode classNode = (ClassNode) node;
            if (classNode.parent != null) {
                getAllSuperAndChildClass(classNode.parent.entity.name, classNames, false);
            }
            if (travelChildren && classNode.children != null) {
                for (ClassNode child : classNode.children) {
                    getAllSuperAndChildClass(child.entity.name, classNames, travelChildren);
                }
            }
        }
        if (node.interfaces != null) {
            for (InterfaceNode anInterface : node.interfaces) {
                getAllSuperAndChildClass(anInterface.entity.name, classNames, false);
            }
        }
    }


    public void release() {
        constFieldNodes.clear();
        runtimeConstFields.clear();
        reflectFieldModels.clear();
        stringPool.clear();
        unknownClassNameReflectFieldNames.clear();
        getFieldReflectModels.clear();
        getFieldsReflectClassNames.clear();
        getDeclaredFieldReflectFields.clear();
        getDeclaredFieldsReflectClassNames.clear();
        whiteListPatterns.clear();
        skipWithAnnotations.clear();
        skipAnnotationClasses.clear();
    }
}
