package com.ss.android.ugc.bytex.refercheck.log;

import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.refercheck.InaccessibleNode;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.gradle.api.artifacts.Configuration;

import java.io.File;

/**
 * Created by tanlehua on 2019/4/15.
 */
class PinpointProblemAnalyzer {
    private Graph graph;
    private GradleCacheResolver cacheResolver;
    private DependencyGraph dependencyGraph;
    private SAXReader reader = new SAXReader();

    PinpointProblemAnalyzer(Graph graph, GradleCacheResolver cacheResolver, Configuration configuration) {
        this.graph = graph;
        this.cacheResolver = cacheResolver;
        this.dependencyGraph = new DependencyGraph(configuration);
    }

    String analyze(InaccessibleNode inaccessibleNode) {
        Node ownerNode = graph.get(inaccessibleNode.memberClassName);
        if (ownerNode == null) {
            return String.format("Tips: class %s is not existed， please checkout why this class were not be included to project build.\n " +
                    "%s 这个类没被引进项目构建里，请检查是否有问题。\n", inaccessibleNode.memberClassName, inaccessibleNode.memberClassName);
        }

        if (dependencyGraph == null || cacheResolver == null) return "";
        Artifact artifactWithCallLocation = new Artifact(inaccessibleNode.callClassName);
        if (artifactWithCallLocation.invalid()) return "";
        StringBuilder tips = new StringBuilder(String.format("Tips: class %s is in aar %s:%s:%s. \n", inaccessibleNode.callClassName,
                artifactWithCallLocation.groupId, artifactWithCallLocation.artifactId, artifactWithCallLocation.version));
        Artifact artifactWithOwner = new Artifact(inaccessibleNode.memberClassName);
        if (!artifactWithOwner.invalid()) {
            tips.append(String.format("class %s is in aar %s:%s:%s. \n", inaccessibleNode.memberClassName, artifactWithOwner.groupId, artifactWithOwner.artifactId, artifactWithOwner.version));
            tips.append(dependencyGraph.render(artifactWithCallLocation, artifactWithOwner));
        }
        return tips.toString();
    }

    class Artifact {
        String groupId;
        String artifactId;
        String version;

        Artifact(String className) {
            GradleCacheResolver.ClassFile classFile = cacheResolver.get(className);
            if (classFile != null) {
                try {
                    File pom = findPomFile(classFile.jar);
                    if (!pom.isFile()) {
                        String[] sp = classFile.jar.split("/");
                        groupId = sp[sp.length - 5];
                        artifactId = sp[sp.length - 4];
                        version = sp[sp.length - 3];
                    } else {
                        Document document = reader.read(pom);
                        Element root = document.getRootElement();
                        groupId = root.elementText("groupId");
                        artifactId = root.elementText("artifactId");
                        version = root.elementText("version");
                    }

                } catch (Exception e) {
                    groupId = null;
                    artifactId = null;
                    version = null;
                }
            }
        }

        private File findPomFile(String jar) {
            String pomPath = jar.substring(0, jar.lastIndexOf(".")) + ".pom";
            return new File(pomPath);
        }

        boolean invalid() {
            return groupId == null || artifactId == null || version == null;
        }
    }
}
