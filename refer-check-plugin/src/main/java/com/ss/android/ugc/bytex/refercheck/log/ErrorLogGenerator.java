package com.ss.android.ugc.bytex.refercheck.log;

import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.refercheck.ReferCheckContext;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferenceLocation;
import com.ss.android.ugc.bytex.transformer.TransformEngine;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by tanlehua on 2019/4/15.
 */
public class ErrorLogGenerator {
    private final ReferCheckContext context;
    private final TransformEngine transformer;
    private final Project project;
    private Map<String, ReferenceLocation> notFoundMethods;
    private Map<String, ReferenceLocation> notFoundFields;

    public ErrorLogGenerator(ReferCheckContext context, TransformEngine transformer, Project project) {
        this.context = context;
        this.transformer = transformer;
        this.project = project;
        this.notFoundMethods = context.getNotFoundMethods();
        this.notFoundFields = context.getNotFoundFields();
    }

    public String generate() {
        if (!notFoundMethods.isEmpty() || !notFoundFields.isEmpty()) {
            String variantName = transformer.getContext().getVariantName();
            Graph graph = context.getClassGraph();
            PinpointProblemAnalyzer problemResolveAnalyzer = getPinpointProblemAnalyzer(variantName, graph);
            StringBuilder sb = new StringBuilder("I checkout some methods are not found or inaccessible in the project while compiling and building, please review your code and library dependencies to figure out why they are not found. Any question feel free to contact @tanlehua. \n" +
                    "我在编译构建过程中检查出有些方法或字段访问不到，辛苦你review一下代码和库的依赖关系，看看为啥这些方法或字段在编译构建时不存在。" +
                    String.format("Run ./gradlew app:dependencies --configuration %sRuntimeClasspath to get more detail about project dependencies graph.\n", variantName) +
                    "We advise you to copy those log below, and leverage the \'Analyse Stacktrace\' in AndroidStudio to locate specific classes and methods.\n" +
                    "建议你把下面类堆栈的日志copy下来，利用AndroidStudio的Analyse Stacktrace可以定位到具体的Class和Method。\n" +
                    "If you're building your apk locally, please make sure you've appended \'--no-daemon\' to the build command. \n" +
                    "如果你用的是本地命令行打包，请你在打包命令后面拼上--no-daemon再试试。\n" +
                    "More information is available in this docs: https://bytedance.feishu.cn/space/doc/doccnkJ1tXrNPkoZnp9eu77h6Eb#f2VZTB \n" +
                    turn2Helper());
            Set<String> relativeClasses = new HashSet<>();
            notFoundMethods.forEach((k, v) -> {
                String[] split = k.split(ReferCheckContext.SEPARATOR);
                String owner = split[0];
                String methodName = split[1];
                String desc = split[2];
                String errorLog = String.format("[ByteX]: Class: [%s], Method: [%s], Desc: [%s] %s. \n",
                        owner, methodName, desc, v.inaccessible() ? "is inaccessible" : "is not found");
                String errorStack = String.format("             at %s.%s(%s:%s) \n", v.clzLoc.replaceAll("/", "."), v.methodLoc, v.getSourceFile(), String.valueOf(v.line));
                sb.append(errorLog).append(errorStack);
                if (!v.inaccessible()) {
                    Node node = graph.get(owner);
                    if (node == null || !node.defined.get()) {
                        sb.append(String.format("Tips: class [%s] was not packaged, please checkout if it was \'compileOnly\' or excluded by some dependencies.",
                                owner));
                    }
                }
                if (problemResolveAnalyzer != null) {
                    sb.append(problemResolveAnalyzer.analyze(owner, methodName, desc, v))
                            .append("\n");
                }
                relativeClasses.add(Utils.replaceDot2Slash(owner) + ".class");
                relativeClasses.add(Utils.replaceDot2Slash(v.clzLoc) + ".class");
            });

            notFoundFields.forEach((k, v) -> {
                String[] split = k.split(ReferCheckContext.SEPARATOR);
                String owner = split[0];
                String fieldName = split[1];
                String desc = split[2];
                String errorLog = String.format("[ByteX]: Class: [%s], Field: [%s], Desc: [%s] %s. \n",
                        owner, fieldName, desc, v.inaccessible() ? "is inaccessible" : "is not found");
                String errorStack = String.format("             at %s.%s(%s:%s) \n", v.clzLoc.replaceAll("/", "."), v.methodLoc, v.getSourceFile(), String.valueOf(v.line));
                sb.append(errorLog).append(errorStack);
                if (!v.inaccessible()) {
                    Node node = graph.get(owner);
                    if (node == null || !node.defined.get()) {
                        sb.append(String.format("Tips: class [%s] was not packaged, please checkout if it was \'compileOnly\' or excluded by some dependencies.",
                                owner));
                    }
                }
                relativeClasses.add(Utils.replaceDot2Slash(owner) + ".class");
                relativeClasses.add(Utils.replaceDot2Slash(v.clzLoc) + ".class");
            });
            sb.append("\nTips:\n");
            for (String relativeClass : relativeClasses) {
                sb.append(relativeClass).append(":[")
                        .append(Utils.getAllFileCachePath(context.getTransformContext(), relativeClass).replaceAll("\n", "\n\t"))
                        .append("\n]\n");
            }
            return sb.toString();
        }
        return null;
    }

    private PinpointProblemAnalyzer getPinpointProblemAnalyzer(String variantName, Graph graph) {
        PinpointProblemAnalyzer problemResolveAnalyzer = null;
        Configuration configuration = null;
        GradleCacheResolver cacheResolver = null;
        if (context.extension.moreErrorInfo()) {
            try {
                configuration = project.getConfigurations().getByName(String.format("%sRuntimeClasspath", variantName));
                Set<File> files = null;
                try {
                    files = configuration.getFiles();
                } catch (Exception e) {
                    try {
                        Class<? extends Configuration> configurationClass = DefaultConfiguration.class;
                        Field intrinsicFiles = configurationClass.getDeclaredField("intrinsicFiles");
                        if (intrinsicFiles != null) {
                            intrinsicFiles.setAccessible(true);
                            FileCollection intrinsicFilesObj = (FileCollection) intrinsicFiles.get(configuration);
                            Class<?> configurationFileCollectionClass = Class.forName(intrinsicFilesObj.getClass().getName());
                            Field lenient = configurationFileCollectionClass.getDeclaredField("lenient");
                            lenient.setAccessible(true);
                            Object lenientValue = lenient.get(intrinsicFilesObj);
                            lenient.set(intrinsicFilesObj, true);
                            files = configuration.getFiles();
                            lenient.set(intrinsicFilesObj, lenientValue);
                            lenient.setAccessible(false);
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                cacheResolver = new GradleCacheResolver();
                for (File file : files) {
                    cacheResolver.accept(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (context.extension.moreErrorInfo()) {
            problemResolveAnalyzer = new PinpointProblemAnalyzer(graph, cacheResolver, configuration);
        }
        return problemResolveAnalyzer;
    }

    private String turn2Helper() {
        String owner = context.extension.getOwner();
        return owner == null ? "" : String.format("如果有疑难问题，随时@%s~ \nIf you have any question feel free to contact @%s~ \n", owner, owner);
    }

}
