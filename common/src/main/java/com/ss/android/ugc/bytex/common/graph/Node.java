package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class Node implements Jsonable {
    /**
     * parent class node of this class node<br/>
     * it could be null when it doesn't exists actually, it's a virtual class node
     */
    public ClassNode parent; //
    public List<InterfaceNode> interfaces;

    public ClassEntity entity;
    /**
     * flag of whether we have visit the real node<br/>
     * used by duplicate class checker<br/>
     */
    public transient final AtomicBoolean defined = new AtomicBoolean(false);

    public Node(ClassEntity entity, ClassNode parent, List<InterfaceNode> interfaces) {
        this.entity = entity;
        this.parent = parent;
        this.interfaces = interfaces;
    }

    /**
     * Determines if the class or interface represented by this
     * node is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * node parameter.
     * It returns true if so;otherwise it returns  false.
     * much like return node instanceof this
     */
    public final boolean isAssignableFrom(final Node node) {
        if (node == null) return false;
        if (entity.name.equals(node.entity.name)) {
            return true;
        }
        final Queue<Node> interfaces = new LinkedList<>();
        if (node.interfaces != null) {
            interfaces.addAll(node.interfaces);
        }
        Node parent = node.parent;
        while (parent != null) {
            if (entity.name.equals(parent.entity.name)) {
                return true;
            }
            if (parent.interfaces != null) {
                interfaces.addAll(parent.interfaces);
            }
            parent = parent.parent;
        }
        while (!interfaces.isEmpty()) {
            Node aInterface = interfaces.poll();
            if (entity.name.equals(aInterface.entity.name)) {
                return true;
            }
            if (aInterface.interfaces != null) {
                interfaces.addAll(aInterface.interfaces);
            }
        }
        return false;
    }

    /**
     * Determines if the class or interface represented by
     * the specified node parameter is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by this node.
     * It returns true if so;otherwise it returns  false.
     * much like return this instanceof node
     */
    public final boolean inheritFrom(final Node node) {
        if (node == null) return false;
        if (entity.name.equals(node.entity.name)) {
            return true;
        }
        final boolean isInterfaceNode = node instanceof InterfaceNode;
        final Queue<Node> interfaces = new LinkedList<>();
        if (isInterfaceNode && this.interfaces != null) {
            interfaces.addAll(this.interfaces);
        }
        Node parent = this.parent;
        while (parent != null) {
            if (isInterfaceNode) {
                interfaces.addAll(parent.interfaces);
            } else {
                if (node.entity.name.equals(parent.entity.name)) {
                    return true;
                }
            }
            parent = parent.parent;
        }

        while (!interfaces.isEmpty()) {
            Node aInterface = interfaces.poll();
            if (node.entity.name.equals(aInterface.entity.name)) {
                return true;
            }
            if (aInterface.interfaces != null) {
                interfaces.addAll(aInterface.interfaces);
            }
        }
        return false;
    }

    /**
     * One method of a class may be not defined in this class, it probably is a method inherited from
     * a specific ancestor class.
     * So this method is used to find where the method was really defined.
     *
     * @param name method name
     * @param desc method desc
     * @return If the method with specific 'name' and 'desc' is not found, it would return null.
     */
    public MethodEntity confirmOriginMethod(String name, String desc) {
        Queue<Node> interfaces = new LinkedList<>();
        Node node = this;
        while (node != null) {
            Optional<MethodEntity> findMethod = node.entity.methods.stream()
                    .filter(m -> name.equals(m.name()) && desc.equals(m.desc()))
                    .findAny();
            if (findMethod.isPresent()) {
                MethodEntity realMethod = findMethod.get();
                String owner = node.entity.name;
                if (TypeUtil.isPrivate(realMethod.access) && !owner.equals(realMethod.className)) {
                    return null;
                }
                return realMethod;
            }
            node.interfaces.forEach(interfaces::offer);
            node = node.parent;
        }
        MethodEntity candidate = null;
        while (!interfaces.isEmpty()) {
            Node interfaceNode = interfaces.poll();
            if (interfaceNode == null) continue;
            Optional<MethodEntity> findMethod = interfaceNode.entity.methods.stream()
                    .filter(m -> name.equals(m.name()) && desc.equals(m.desc()))
                    .findAny();
            if (findMethod.isPresent()) {
                MethodEntity realMethod = findMethod.get();
                if (TypeUtil.isAbstract(realMethod.access)) {
                    candidate = realMethod;
                    continue;
                }
                return realMethod;
            }
            if (interfaceNode.interfaces != null) {
                interfaceNode.interfaces.forEach(interfaces::offer);
            }
        }
        return candidate;
    }

    /**
     * One field of a class may be not defined in this class, it probably is a field inherited from
     * a specific ancestor class.
     * So this method is used to find where the field with specific 'name' and 'desc' is really defined.
     *
     * @param name method name
     * @param desc method desc
     * @return If the method with specific 'name' and 'desc' is not found, it would return null.
     */
    public FieldEntity confirmOriginField(String name, String desc) {
        Node node = this;
        Queue<Node> interfaces = new LinkedList<>();
        while (node != null) {
            ClassEntity entity = node.entity;
            String owner = entity.name;
            Optional<FieldEntity> findField = entity.fields.stream()
                    .filter(f -> (!TypeUtil.isPrivate(f.access) || f.className().equals(owner))
                            && f.name().equals(name) && f.desc().equals(desc))
                    .findAny();
            if (findField.isPresent()) {
                return findField.get();
            }
            node.interfaces.forEach(interfaces::offer);
            node = node.parent;
        }
        while (!interfaces.isEmpty()) {
            Node interfaceNode = interfaces.poll();
            if (interfaceNode == null) continue;
            Optional<FieldEntity> findMethod = interfaceNode.entity.fields.stream()
                    .filter(f -> name.equals(f.name()) && desc.equals(f.desc()))
                    .findAny();
            if (findMethod.isPresent()) {
                return findMethod.get();
            }
            if (interfaceNode.interfaces != null) {
                interfaceNode.interfaces.forEach(interfaces::offer);
            }
        }
        return null;
    }

    @Override
    public void read(JsonReader jsonReader, Gson gson) throws IOException {
        throw new UnsupportedOperationException("Jsonable Not Supported");
    }

    @Override
    public void write(JsonWriter jsonWriter, Gson gson) throws IOException {
        throw new UnsupportedOperationException("Jsonable Not Supported");
    }
}
