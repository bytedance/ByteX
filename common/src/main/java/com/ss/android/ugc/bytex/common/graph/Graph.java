package com.ss.android.ugc.bytex.common.graph;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Class dependency graph.
 */
public class Graph {


    private final Map<String, Node> nodeMap;

    Graph(Map<String, Node> nodesMap) {
        this.nodeMap = nodesMap;
    }

    /**
     * thread unsafe
     * Before prepare, the Graph only has vector from child to super.
     * this method will add vector from super to child.
     * After prepare, there is a full graph.
     */
    void prepare() {
        nodeMap.values()
                .forEach(n -> {
                    if (n.parent != null) {
                        ClassNode parent = n.parent;
                        if (parent.children == Collections.EMPTY_LIST) {

                            // optimize for Object
                            if (parent.entity.name.equals("java/lang/Object")) {
                                parent.children = new ArrayList<>(nodeMap.size() >> 1);
                            } else {
                                parent.children = new ArrayList<>();
                            }
                        }
                        // all interfaces extends java.lang.Object
                        // make java.lang.Object subclasses purely
                        if (n instanceof ClassNode) {
                            parent.children.add((ClassNode) n);
                        }
                    }
                    n.interfaces.forEach(i -> {
                        if (n instanceof InterfaceNode) {
                            if (i.children == Collections.EMPTY_LIST) {
                                i.children = new ArrayList<>();
                            }
                            i.children.add((InterfaceNode) n);
                        } else {
                            if (i.implementedClasses == Collections.EMPTY_LIST) {
                                i.implementedClasses = new ArrayList<>();
                            }
                            //noinspection ConstantConditions
                            i.implementedClasses.add((ClassNode) n);
                        }
                    });
                });
    }

    /**
     * clear graph info
     * called by internal,please do not call.
     */
    public void clear() {
        nodeMap.clear();
    }

    public boolean inherit(String child, String parent) {
        Node childNode = nodeMap.get(child);
        Node parentNode = nodeMap.get(parent);
        return childNode != null && childNode.inheritFrom(parentNode);
    }


    public Node get(String className) {
        return nodeMap.get(className);
    }


    /**
     * Returns the common super type of the two given types.
     *
     * @param type1 the internal name of a class.
     * @param type2 the internal name of another class.
     * @return the internal name of the common super class of the two given
     * classes.
     */
    public String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        Node node = nodeMap.get(type1);
        if (node == null) {
            throw new TypeNotPresentException(type1, null);
        }
        Node node2 = nodeMap.get(type2);
        if (node2 == null) {
            throw new TypeNotPresentException(type2, null);
        }
        if (node.isAssignableFrom(node2)) {
            return type1;
        }

        if (node2.isAssignableFrom(node)) {
            return type2;
        }
        if (node instanceof InterfaceNode || node2 instanceof InterfaceNode) {
            return "java/lang/Object";
        } else {
            do {
                node = node.parent;
            } while (!node.isAssignableFrom(node2));
            return node.entity.name.replace('.', '/');
        }
    }

    public List<ClassNode> implementsOf(String interfaceName) {
        Node node = nodeMap.get(interfaceName);
        if (node == null) {
            return Collections.emptyList();
        } else if (!(node instanceof InterfaceNode)) {
            throw new IllegalArgumentException(interfaceName + " is not a interface");
        }
        InterfaceNode realNode = (InterfaceNode) node;
        return realNode.implementedClasses;
    }

    public boolean implementOf(String child, String interfaceName) {
        Node node = nodeMap.get(interfaceName);
        if (node == null) {
            return false;
        } else if (!(node instanceof InterfaceNode)) {
            return false;
        }
        InterfaceNode interfaceNode = (InterfaceNode) node;
        final boolean[] found = {false};
        traverseChildren(interfaceNode, n -> {
            if (child.equals(n.entity.name)) {
                found[0] = true;
                return true;
            }
            return false;
        });
        return found[0];
    }

    public List<ClassNode> childrenOf(String className) {
        Node node = nodeMap.get(className);
        if (node == null) {
            return Collections.emptyList();
        } else if (!(node instanceof ClassNode)) {
            throw new IllegalArgumentException(className + " is not a interface");
        }
        ClassNode classNode = (ClassNode) node;
        List<ClassNode> children = new ArrayList<>();
        traverseAllChild(classNode, children::add);
        return children;
    }

    public boolean instanceofClass(String className, String targetClassName) throws ClassNotFoundException {
        Node child = get(className);
        if (child == null) {
            throw new ClassNotFoundException(String.format("class %s not found!", className));
        }
        Node parent = get(targetClassName);
        if (parent == null) {
            throw new ClassNotFoundException(String.format("class %s not found!", targetClassName));
        }
        return child.inheritFrom(parent);
    }

    // TODO: 2019/3/13 should be optimized
    public void traverseAllChild(ClassNode classNode, Consumer<ClassNode> visitor) {
        // bfs
        Queue<ClassNode> handleQ = new LinkedList<>();
        classNode.children.forEach(handleQ::offer);
        while (!handleQ.isEmpty()) {
            ClassNode n = handleQ.poll();
            visitor.accept(n);
            n.children.forEach(handleQ::offer);
        }
    }

    public void traverseChildren(ClassNode classNode, Function<ClassNode, Boolean> visitor) {
        // bfs
        Queue<ClassNode> handleQ = new LinkedList<>();
        classNode.children.forEach(handleQ::offer);
        while (!handleQ.isEmpty()) {
            ClassNode n = handleQ.poll();
            if (visitor != null && visitor.apply(n)) {
                break;
            }
            n.children.forEach(handleQ::offer);
        }
    }

    public void traverseChildren(InterfaceNode interfaceNode, Function<Node, Boolean> visitor) {
        // bfs
        Queue<Node> handleQ = new LinkedList<>();
        interfaceNode.children.forEach(handleQ::offer);
        interfaceNode.implementedClasses.forEach(handleQ::offer);
        while (!handleQ.isEmpty()) {
            Node n = handleQ.poll();
            if (visitor != null && visitor.apply(n)) {
                break;
            }
            if (n instanceof InterfaceNode) {
                ((InterfaceNode) n).children.forEach(handleQ::offer);
                ((InterfaceNode) n).implementedClasses.forEach(handleQ::offer);
            } else if (n instanceof ClassNode) {
                ((ClassNode) n).children.forEach(handleQ::offer);
            }
        }
    }

    public void backtrackToParent(ClassNode classNode, Function<ClassNode, Boolean> visitor) {
        while (classNode != null) {
            if (visitor != null && visitor.apply(classNode)) {
                break;
            }
            classNode = classNode.parent;
        }
    }

    /**
     * judge the method whether it overrides from superior or interfaces.
     */
    public boolean overrideFromSuper(String className, String methodName, String desc) {
        Node classNode = get(className);
        if (classNode == null) throw new RuntimeException("No such method : " + methodName);
        if (isMethodFromInterface(methodName, desc, classNode)) return true;
        while (classNode.parent != null) {
            ClassEntity parent = classNode.parent.entity;
            if (parent.methods.stream().anyMatch(m -> m.name().equals(methodName) && m.desc().equals(desc))) {
                return true;
            }
            if (isMethodFromInterface(methodName, desc, classNode)) return true;
            classNode = classNode.parent;
        }
        return false;
    }

    private boolean isMethodFromInterface(String methodName, String desc, Node classNode) {
        Queue<InterfaceNode> handleQ = new LinkedList<>();
        classNode.interfaces.forEach(handleQ::offer);
        while (!handleQ.isEmpty()) {
            InterfaceNode n = handleQ.poll();
            if (n.entity.methods.stream().anyMatch(m -> m.name().equals(methodName) && m.desc().equals(desc))) {
                return true;
            }
            n.interfaces.forEach(handleQ::offer);
        }
        return false;
    }

    /**
     * Judge the method whether it is overrided by children.
     */
    public boolean overridedBySubclass(String className, String methodName, String desc) {
        ClassNode classNode = (ClassNode) get(className);
        if (classNode == null) throw new RuntimeException("No such method : " + methodName);
        AtomicBoolean found = new AtomicBoolean(false);
        traverseChildren(classNode, child -> {
            ClassEntity childEntity = child.entity;
            for (MethodEntity m : childEntity.methods) {
                if (m.name().equals(methodName) && m.desc().equals(desc)) {
                    found.set(true);
                    return true;
                }
            }
            if (isMethodFromInterface(methodName, desc, child)) {
                found.set(true);
                return true;
            }
            return false;
        });
        return found.get();
    }

    public FieldEntity confirmOriginField(String owner, String name, String desc) {
        Node node = get(owner);
        if (node == null) return null;
        return node.confirmOriginField(name, desc);
    }

    public MethodEntity confirmOriginMethod(String owner, String name, String desc) {
        Node node = get(owner);
        if (node == null) {
            return null;
        }
        return node.confirmOriginMethod(name, desc);
    }

    public Map<String, Node> getNodes(){
        return Collections.unmodifiableMap(nodeMap);
    }
}
