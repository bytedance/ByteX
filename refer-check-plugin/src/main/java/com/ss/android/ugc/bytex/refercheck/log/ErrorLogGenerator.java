package com.ss.android.ugc.bytex.refercheck.log;

import com.ss.android.ugc.bytex.refercheck.InaccessibleNode;

import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Created by tanlehua on 2019/4/15.
 */
public class ErrorLogGenerator {
    @Nullable
    private final PinpointProblemAnalyzer problemResolveAnalyzer;
    @Nullable
    private final TipsProvider tipsProvider;
    private final String variantName;
    @Nullable
    private final String owner;
    private final List<InaccessibleNode> inaccessableMethods;
    private final List<InaccessibleNode> inaccessableFields;

    public ErrorLogGenerator(@Nullable PinpointProblemAnalyzer problemResolveAnalyzer, @Nullable TipsProvider tipsProvider, String variantName, @Nullable String owner,
                             List<InaccessibleNode> inaccessableMembers) {
        this.problemResolveAnalyzer = problemResolveAnalyzer;
        this.tipsProvider = tipsProvider;
        this.variantName = variantName;
        this.owner = owner;
        this.inaccessableMethods = inaccessableMembers.stream().filter(inaccessibleNode -> Type.getType(inaccessibleNode.memberDesc).getSort() == Type.METHOD).collect(Collectors.toList());
        this.inaccessableFields = inaccessableMembers.stream().filter(inaccessibleNode -> Type.getType(inaccessibleNode.memberDesc).getSort() != Type.METHOD).collect(Collectors.toList());
    }

    public String generate() {
        if (!inaccessableMethods.isEmpty() || !inaccessableFields.isEmpty()) {
            StringBuilder sb = new StringBuilder("I checkout some methods are not found or inaccessible in the project while compiling and building, please review your code and library dependencies to figure out why they are not found. Any question feel free to contact @yangzhiqian. \n" +
                    "我在编译构建过程中检查出有些方法或字段访问不到，辛苦你review一下代码和库的依赖关系，看看为啥这些方法或字段在编译构建时不存在。" +
                    String.format("Run ./gradlew app:dependencies --configuration %sRuntimeClasspath to get more detail about project dependencies graph.\n", variantName) +
                    "We advise you to copy those log below, and leverage the \'Analyse Stacktrace\' in AndroidStudio to locate specific classes and methods.\n" +
                    "建议你把下面类堆栈的日志copy下来，利用AndroidStudio的Analyse Stacktrace可以定位到具体的Class和Method。\n" +
                    "If you're building your apk locally, please make sure you've appended \'--no-daemon\' to the build command. \n" +
                    "如果你用的是本地命令行打包，请你在打包命令后面拼上--no-daemon再试试。\n" +
                    turn2Helper());
            Set<String> relativeClasses = new HashSet<>();
            for (InaccessibleNode method : inaccessableMethods) {
                sb.append(method.toString()).append("\n");
                if (problemResolveAnalyzer != null) {
                    sb.append(problemResolveAnalyzer.analyze(method)).append("\n");
                }
                relativeClasses.add(method.callClassName + ".class");
                relativeClasses.add(method.memberClassName + ".class");
            }
            for (InaccessibleNode field : inaccessableFields) {
                sb.append(field.toString()).append("\n");
                if (problemResolveAnalyzer != null) {
                    sb.append(problemResolveAnalyzer.analyze(field)).append("\n");
                }
                relativeClasses.add(field.callClassName + ".class");
                relativeClasses.add(field.memberClassName + ".class");
            }
            if (tipsProvider != null) {
                sb.append("\nTips:\n");
                for (String relativeClass : relativeClasses) {
                    sb.append(relativeClass).append(":[")
                            .append(tipsProvider.provideFilePathInfo(relativeClass).replaceAll("\n", "\n\t"))
                            .append("\n]\n");
                }
            }
            return sb.toString();
        }
        return null;
    }

    private String turn2Helper() {
        return owner == null ? "" : String.format("如果有疑难问题，随时@%s~ \nIf you have any question feel free to contact @%s~ \n", owner, owner);
    }

}
