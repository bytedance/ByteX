package com.ss.android.ugc.bytex.coverage_plugin.util;

import com.google.gson.Gson;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.coverage_plugin.Context;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jiangzilai on 2019-07-16.
 * 将类图序列化到文件中
 */
public class GraphUtil {

    public static void saveGraph(Context context) throws IOException, NoSuchFieldException, IllegalAccessException {
        Map<String, ClassNodeBean> classNodeBeanMap = buildNodeMap(context);
        System.out.println("graph build complete");
        String json = new Gson().toJson(classNodeBeanMap);
        System.out.println("graph Json init complete");
        FileUtil.saveFile(json, context.getGraphFilePath());
        System.out.println("graph save complete:" + context.getGraphFilePath());
        classNodeBeanMap = null;
    }

    private static Map<String, ClassNodeBean> buildNodeMap(Context context) throws NoSuchFieldException, IllegalAccessException {
        if (context == null) return null;
        Map<String, Node> nodeMap = context.getClassGraph().getNodes();
        Map<String, ClassNodeBean> map = new HashMap<>(nodeMap.size());
        // 一定要完完整整地复制...少了会导致统计不准
        for (Map.Entry<String, Node> stringNodeEntry : nodeMap.entrySet()) {
            Node node = stringNodeEntry.getValue();
            if (node instanceof ClassNode) {
                List<String> children = new ArrayList<>(((ClassNode) node).children.size());
                for (ClassNode child : ((ClassNode) node).children) {
                    children.add(child.entity.name);
                }
                map.put(stringNodeEntry.getKey(), new ClassNodeBean(node.entity.name, children, node.entity.superName));
            }
        }
        return map;
    }

    // 单向链表，Gson序列化双向时会出问题
    // use one-way LinkedList for serialization
    private static class ClassNodeBean implements Serializable {

        private String className;
        private List<String> children;

        // parent 可能是interface
        private String parent;

        public ClassNodeBean(String className, List<String> children, String parent) {
            this.className = className;
            this.children = children;
            this.parent = parent;
        }

        @Override public String toString() {
            return "ClassNodeBean{" +
                    "className='" + className + '\'' +
                    ", children=" + children +
                    ", parent='" + parent + '\'' +
                    '}';
        }
    }
}
