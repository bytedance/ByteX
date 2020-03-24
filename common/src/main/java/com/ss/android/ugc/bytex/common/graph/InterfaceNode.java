package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ss.android.ugc.bytex.common.graph.cache.GraphTypeAdapterFactory;

import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class InterfaceNode extends Node {

    public List<InterfaceNode> children = Collections.emptyList();
    public List<ClassNode> implementedClasses = Collections.emptyList();

    public InterfaceNode(String className) {
        super(new ClassEntity(className, Opcodes.ACC_INTERFACE), null, Collections.emptyList());
    }

    @Override
    public void read(JsonReader jsonReader, Gson gson) throws IOException {
        if (this.getClass() != InterfaceNode.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "parent":
                case "p":
                    parent = gson.getAdapter(ClassNode.class).read(jsonReader);
                    break;
                case "entity":
                case "e":
                    entity = gson.getAdapter(ClassEntity.class).read(jsonReader);
                    break;
                case "interfaces":
                case "i":
                    interfaces = GraphTypeAdapterFactory.readList(jsonReader, gson, InterfaceNode.class);
                    break;
                case "children":
                case "c":
                    children = GraphTypeAdapterFactory.readList(jsonReader, gson, InterfaceNode.class);
                    break;
                case "implementedClasses":
                case "ic":
                    implementedClasses = GraphTypeAdapterFactory.readList(jsonReader, gson, ClassNode.class);
                    break;
                default:
                    throw new RuntimeException("unsupport json name:" + name);
            }
        }
        jsonReader.endObject();
    }

    @Override
    public void write(JsonWriter jsonWriter, Gson gson) throws IOException {
        if (this.getClass() != InterfaceNode.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonWriter.beginObject();
        if (parent != null) {
            jsonWriter.name("p");
            gson.getAdapter(ClassNode.class).write(jsonWriter, parent);
        }
        if (entity != null) {
            jsonWriter.name("e");
            gson.getAdapter(ClassEntity.class).write(jsonWriter, entity);
        }

        if (interfaces != null && !interfaces.isEmpty()) {
            jsonWriter.name("i");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, interfaces);
        }

        if (children != null && !children.isEmpty()) {
            jsonWriter.name("c");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, children);
        }

        if (implementedClasses != null && !implementedClasses.isEmpty()) {
            jsonWriter.name("ic");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, implementedClasses);
        }
        jsonWriter.endObject();
    }
}
