package com.ss.android.ugc.bytex.refercheck.log;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by tanlehua on 2019/4/15.
 */
class DependencyGraph {
    private final Map<String, List<Dependency>> dependencies = new HashMap<>();

    DependencyGraph(Configuration configuration) {
        ResolutionResult result = configuration.getIncoming().getResolutionResult();
        RenderableDependency root = new RenderableModuleResult(result.getRoot());
        resolve(root, null);
    }

    private List<Dependency> get(String groupId, String artifactId) {
        List<Dependency> dependencies = this.dependencies.get(Dependency.key(groupId, artifactId));
        return dependencies == null ? Collections.emptyList() : dependencies;
    }

    private void resolve(RenderableDependency root, Dependency parent) {
        for (RenderableDependency child : root.getChildren()) {
            Dependency dc = new Dependency(child.getName());
            dc.parent = parent;
            if (parent != null) {
                parent.children.add(dc);
            }
            dependencies.computeIfAbsent(dc.key(), k -> new ArrayList<>())
                    .add(dc);
            resolve(child, dc);
        }
    }

    String render(PinpointProblemAnalyzer.Artifact m, PinpointProblemAnalyzer.Artifact n) {
        int level = 0;
        Render render = new Render();
        if (!m.invalid()) {
            render.drawLine(++level, String.format("%s:%s:%s", m.groupId, m.artifactId, m.version));
        }
        if (!n.invalid()) {
            List<Dependency> dependencies = get(n.groupId, n.artifactId);
            Optional<Dependency> optional = dependencies.stream()
                    .filter(dp -> derivedFrom(dp, m)).findFirst();
            Dependency dependency = null;
            if (optional.isPresent()) {
                dependency = optional.get();
            } else if (!dependencies.isEmpty()) {
                dependency = dependencies.get(0);
            }
            if (dependency != null) {
                if (!(n.groupId.equals(m.groupId) && n.artifactId.equals(m.artifactId))) {
                    doRender(render, ++level, dependency);
                }
                renderChildren(render, level, dependency);
            }
        }
        return render.toString();
    }

    private void renderChildren(Render render, int level, Dependency dependency) {
        for (Dependency child : dependency.children) {
            doRender(render, level, child);
            renderChildren(render, level + 1, child);
        }
    }

    private void doRender(Render render, int level, Dependency dependency) {
        if (dependency.diffVersion()) {
            List<String> list = get(dependency.groupId, dependency.artifactId).stream()
                    .filter(dp -> dp.requestedVersion.equals(dp.actualVersion))
                    .map(this::getDependencyChain)
                    .collect(Collectors.toList());
            String versionUpgradeReport;
            if (!list.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (String s : list) {
                    sb.append(s);
                    if (i++ != list.size() - 1) {
                        sb.append(", ");
                    }
                }
                versionUpgradeReport = sb.toString();
            } else {
                versionUpgradeReport = "*";
            }
            render.drawLine(level, String.format("%s:%s (upgrade by %s)", dependency.key(), dependency.printVersion(), versionUpgradeReport));
        }
    }

    private String getDependencyChain(Dependency dp) {
        StringBuilder sb = new StringBuilder();
        while (dp.parent != null) {
            if (sb.length() > 0) {
                sb.append("->");
            }
            sb.append(String.format("%s:%s", dp.parent.key(), dp.parent.actualVersion));
            dp = dp.parent;
        }
        return sb.toString();
    }

    private boolean derivedFrom(Dependency dp, PinpointProblemAnalyzer.Artifact artifact) {
        while (dp.parent != null) {
            if (dp.parent.groupId.equals(artifact.groupId)
                    && dp.parent.artifactId.equals(artifact.artifactId)) {
                return true;
            }
            dp = dp.parent;
        }
        return false;
    }

    static class Render {
        private StringBuilder sb = new StringBuilder();

        void drawLine(int level, String line) {
            for (int i = 0; i < level; i++) {
                sb.append(" ");
            }
            sb.append("+-- ").append(line).append("\n");
        }

        public String toString() {
            return sb.toString();
        }
    }

    static class Dependency {
        String groupId;
        String artifactId;
        String actualVersion;
        String requestedVersion;
        Dependency parent;
        List<Dependency> children = new ArrayList<>();
        private String name;

        Dependency(String name) {
            try {
                String[] versionSplit = name.split("->");
                name = versionSplit[0].trim();
                String[] nameSplit = name.split(":");
                this.groupId = nameSplit[0];
                this.artifactId = nameSplit[1];
                if (versionSplit.length == 1) {
                    this.requestedVersion = this.actualVersion = nameSplit[2];
                } else {
                    this.actualVersion = versionSplit[1].trim();
                    this.requestedVersion = nameSplit[2];
                }
            } catch (Exception e) {
                this.name = name;
            }
        }

        String key() {
            if (groupId == null || artifactId == null) return name;
            return key(groupId, artifactId);
        }

        static String key(String groupId, String artifactId) {
            return String.format("%s:%s", groupId, artifactId);
        }

        String printVersion() {
            if (!diffVersion()) {
                return actualVersion;
            } else {
                return String.format("%s -> %s", requestedVersion, actualVersion);
            }
        }

        private boolean diffVersion() {
            return !actualVersion.equals(requestedVersion);
        }
    }
}
