package com.ss.android.ugc.bytex.hookproguard;

import com.android.build.gradle.internal.pipeline.TransformTask;
import com.android.build.gradle.internal.transforms.ProGuardTransform;
import com.android.build.gradle.internal.transforms.ProguardConfigurable;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.Node;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import proguard.ClassSpecification;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.KeepClassSpecification;
import proguard.MemberSpecification;
import proguard.util.ClassNameParser;
import proguard.util.ListParser;
import proguard.util.NameParser;

public class ProguardConfigurationAnalyzer {
    private Configuration configuration;
    private ListParser classNameParser = new ListParser(new ClassNameParser());
    private ListParser nameParser = new ListParser(new NameParser());
    // keep the whole class. 这些配置都是keep整个类的
    private final List<KeepClassSpecificationHolder> wholeClassSpecifications = new ArrayList<>();
    // keep some specific methods in a class. 这些配置是用来keep某些类里的某些方法的
    private final List<KeepClassSpecificationHolder> classSpecificationsForMethod = new ArrayList<>();
    // specification with the class hierarchy
    private final List<KeepClassSpecificationHolder> classHierarchySpecifications = new ArrayList<>();
    private static final MemberSpecification defaultMemberSpecification = new MemberSpecification();
    private static ProGuardTransform proGuardTransform;
    private static volatile ProguardConfigurationAnalyzer INSTANCE;

    private ProguardConfigurationAnalyzer() {
    }

    public static ProguardConfigurationAnalyzer hook(Project project) {
        if (INSTANCE == null) {
            synchronized (ProguardConfigurationAnalyzer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProguardConfigurationAnalyzer();
                    project.getGradle().getTaskGraph().addTaskExecutionGraphListener(taskGraph -> {
                        for (Task task : taskGraph.getAllTasks()) {
                            if (task instanceof TransformTask
                                    && ((TransformTask) task).getTransform() instanceof ProGuardTransform) {
                                proGuardTransform = (ProGuardTransform) ((TransformTask) task).getTransform();
                                break;
                            }
                        }
                    });
                    project.getGradle().buildFinished(p -> INSTANCE = null);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void prepare() {
        parseProguardRules(proGuardTransform);
    }


    private void parseProguardRules(ProGuardTransform transform) {
        if (configuration != null) return;
        if (transform == null) {
            throw new RuntimeException("This plugin can only be applied in release build or when proguard is enabled.");
        }
        try {
            configuration = new Configuration();
            Method method = ProguardConfigurable.class.getDeclaredMethod("getAllConfigurationFiles");
            method.setAccessible(true);
            FileCollection files = (FileCollection) method.invoke(transform);
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                System.out.println("[ProguardConfigurationAnalyzer] proguard configuration file : " + file.getAbsolutePath());
                ConfigurationParser parser = new ConfigurationParser(file, System.getProperties());
                try {
                    parser.parse(configuration);
                } finally {
                    parser.close();
                }
            }
            filterAndSplit(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void filterAndSplit(Configuration configuration) {
        if (configuration.keep == null) return;
        List<KeepClassSpecification> newKeepList = new ArrayList<>();
        for (KeepClassSpecification specification : (List<KeepClassSpecification>) configuration.keep) {
            if (specification.allowObfuscation || specification.allowOptimization || specification.allowShrinking) {
                continue;
            }
            if (specification.methodSpecifications == null || specification.methodSpecifications.isEmpty()) {
                continue;
            }
            boolean specificationOnlyForInitializationMethod = true;
            List<MemberSpecification> methodList = specification.methodSpecifications;
            for (MemberSpecification memberSpecification : methodList) {
                if (!"<init>".equals(memberSpecification.name)) {
                    specificationOnlyForInitializationMethod = false;
                    break;
                }
            }
            if (specificationOnlyForInitializationMethod) {
                continue;
            }

            KeepClassSpecificationHolder specificationHolder = new KeepClassSpecificationHolder(specification, classNameParser);
            if (isSpecificationMatchAllMethods(methodList)) {
                wholeClassSpecifications.add(specificationHolder);
            } else {
                specificationHolder.parserMethodSpecifications(nameParser);
                classSpecificationsForMethod.add(specificationHolder);
            }
            if (specification.extendsClassName != null) {
                classHierarchySpecifications.add(specificationHolder);
            }
            newKeepList.add(specification);
        }
        configuration.keep = newKeepList;
    }

    private boolean isSpecificationMatchAllMethods(List<MemberSpecification> methodList) {
        return methodList.size() == 1 && defaultMemberSpecification.equals(methodList.get(0));
    }

    public boolean shouldKeep(ClassInfo classInfo) {
        for (KeepClassSpecificationHolder specification : wholeClassSpecifications) {
            KeepClassSpecification realSpecification = specification.getInstance();
            if (realSpecification.className != null && !specification.match(classInfo.getName()))
                continue;
            if (realSpecification.extendsClassName != null) {
                if (!matchExtendsClass(classInfo, realSpecification)) continue;
            }
            if (realSpecification.annotationType != null) {
                if (!matchAnnotation(classInfo, realSpecification)) continue;
            }
            if (realSpecification.requiredSetAccessFlags != 0) {
                if (!matchSetAccessFlags(classInfo, realSpecification)) continue;
            }
            if (realSpecification.requiredUnsetAccessFlags != 0) {
                if (!matchUnSetAccessFlags(classInfo, realSpecification)) continue;
            }
            return true;
        }
        return false;
    }

    private boolean matchExtendsClass(ClassInfo classInfo, KeepClassSpecification realSpecification) {
        boolean match = false;
        if (realSpecification.extendsClassName.equals(classInfo.getSuperName()))
            match = true;
        if (!match && classInfo.getInterfaces() != null) {
            for (String itf : classInfo.getInterfaces()) {
                if (realSpecification.extendsClassName.equals(itf)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    private boolean matchAnnotation(ClassInfo classInfo, KeepClassSpecification realSpecification) {
        if (classInfo.getAnnotations() != null) {
            for (String annotation : classInfo.getAnnotations()) {
                if (realSpecification.annotationType.equals(annotation)) return true;
            }
        }
        return false;
    }

    public boolean shouldKeep(ClassInfo classInfo, MethodInfo methodInfo) {
        for (KeepClassSpecificationHolder specification : classSpecificationsForMethod) {
            KeepClassSpecification realSpecification = specification.getInstance();
            if (realSpecification.className != null && !specification.match(classInfo.getName()))
                continue;
            if (realSpecification.extendsClassName != null) {
                if (!matchExtendsClass(classInfo, realSpecification)) continue;
            }
            if (realSpecification.annotationType != null) {
                if (!matchAnnotation(classInfo, realSpecification)) continue;
            }
            List<MemberSpecificationHolder> methodSpecifications = specification.getMethodSpecifications(nameParser);
            if (methodSpecifications != null) {
                if (!matchMethod(methodInfo, methodSpecifications)) continue;
            }
            return true;
        }
        return false;
    }

    private boolean matchMethod(MethodInfo methodInfo, List<MemberSpecificationHolder> methodSpecifications) {
        for (MemberSpecificationHolder specification : methodSpecifications) {
            MemberSpecification realSpecification = specification.getInstance();
            if (realSpecification.annotationType != null) {
                if (!matchAnnotation(methodInfo, realSpecification)) continue;
            }
            if (realSpecification.requiredSetAccessFlags != 0) {
                if (!matchSetAccessFlags(methodInfo, realSpecification)) continue;
            }
            if (realSpecification.requiredUnsetAccessFlags != 0) {
                if (!matchUnSetAccessFlags(methodInfo, realSpecification)) continue;
            }
            if (!specification.match(methodInfo.getName(), methodInfo.getDesc())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean matchSetAccessFlags(MethodInfo methodInfo, MemberSpecification realSpecification) {
        return (methodInfo.getAccess() & realSpecification.requiredSetAccessFlags) == realSpecification.requiredSetAccessFlags;
    }

    private boolean matchUnSetAccessFlags(MethodInfo methodInfo, MemberSpecification realSpecification) {
        return (methodInfo.getAccess() & realSpecification.requiredUnsetAccessFlags) == 0;
    }

    private boolean matchSetAccessFlags(ClassInfo classInfo, ClassSpecification realSpecification) {
        return (classInfo.getAccess() & realSpecification.requiredSetAccessFlags) == realSpecification.requiredSetAccessFlags;
    }

    private boolean matchUnSetAccessFlags(ClassInfo classInfo, ClassSpecification realSpecification) {
        return (classInfo.getAccess() & realSpecification.requiredUnsetAccessFlags) == 0;
    }


    private boolean matchAnnotation(MethodInfo methodInfo, MemberSpecification realSpecification) {
        if (methodInfo.getAnnotations() != null) {
            for (String annotation : methodInfo.getAnnotations()) {
                if (realSpecification.annotationType.equals(annotation)) return true;
            }
        }
        return false;
    }


    public boolean shouldKeep(Graph graph, MethodInfo methodInfo) {
        for (KeepClassSpecificationHolder specification : classHierarchySpecifications) {
            KeepClassSpecification realSpecification = specification.getInstance();
            if (realSpecification.className != null && !specification.match(methodInfo.getClassInfo().getName()))
                continue;
            if (realSpecification.extendsClassName != null) {
                if (!matchExtendsClass(graph, methodInfo.getClassInfo().getName(), specification))
                    continue;
            }
            if (realSpecification.annotationType != null) {
                if (!matchAnnotation(methodInfo.getClassInfo(), realSpecification)) continue;
            }
            List<MemberSpecificationHolder> methodSpecifications = specification.getMethodSpecifications(nameParser);
            if (methodSpecifications != null) {
                if (!matchMethod(methodInfo, methodSpecifications)) continue;
            }
            return true;
        }
        return false;
    }

    public boolean shouldKeep(Graph graph, ClassInfo classInfo) {
        for (KeepClassSpecificationHolder specification : classHierarchySpecifications) {
            KeepClassSpecification realSpecification = specification.getInstance();
            if (realSpecification.className != null && !specification.match(classInfo.name))
                continue;
            if (realSpecification.extendsClassName != null) {
                if (!matchExtendsClass(graph, classInfo.name, specification))
                    continue;
            }
            if (realSpecification.annotationType != null) {
                if (!matchAnnotation(classInfo, realSpecification)) continue;
            }
            return true;
        }
        return false;
    }

    private boolean matchExtendsClass(Graph graph, String className, KeepClassSpecificationHolder specification) {
        Node node = specification.computeExtendsClassNode(graph);
        if (node == null) return false;
        Node derivedClass = graph.get(className);
        return derivedClass != null && derivedClass.inheritFrom(node);
    }
}
