package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ss.android.ugc.bytex.common.graph.cache.GraphTypeAdapterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassEntity implements Jsonable {

    public int access;
    public String name;
    public String superName;
    public List<String> interfaces;

    public List<FieldEntity> fields;
    public List<MethodEntity> methods;
    public boolean fromAndroid;

    // placeholder
    public ClassEntity(String name, int access) {
        this.name = name;
        this.access = access;
        this.interfaces = Collections.emptyList();
        this.fields = Collections.emptyList();
        this.methods = Collections.emptyList();
    }

    // actual
    public ClassEntity(int access, String name, String superName, List<String> interfaces) {
        this.access = access;
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ClassEntity{" +
                "access=" + access +
                ", name='" + name + '\'' +
                ", superName='" + superName + '\'' +
                ", interfaces=" + interfaces +
                ", fields=" + fields +
                ", methods=" + methods +
                '}';
    }


    @Override
    public void read(JsonReader jsonReader, Gson gson) throws IOException {
        if (this.getClass() != ClassEntity.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "access":
                case "a":
                    access = jsonReader.nextInt();
                    break;
                case "name":
                case "n":
                    this.name = jsonReader.nextString();
                    break;
                case "superName":
                case "s":
                    superName = jsonReader.nextString();
                    break;
                case "interfaces":
                case "i":
                    interfaces = GraphTypeAdapterFactory.readList(jsonReader, gson, String.class);
                    break;
                case "fields":
                case "f":
                    fields = GraphTypeAdapterFactory.readList(jsonReader, gson, FieldEntity.class);
                    break;
                case "methods":
                case "m":
                    methods = GraphTypeAdapterFactory.readList(jsonReader, gson, MethodEntity.class);
                    break;
                case "fromAndroid":
                case "e":
                    fromAndroid = jsonReader.nextBoolean();
                    break;
                default:
                    throw new RuntimeException("unsupport json name:" + name);
            }
        }
        jsonReader.endObject();
    }

    @Override
    public void write(JsonWriter jsonWriter, Gson gson) throws IOException {
        if (this.getClass() != ClassEntity.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonWriter.beginObject();
        jsonWriter.name("a").value(access);
        if (name != null) {
            jsonWriter.name("n").value(name);
        }
        if (superName != null) {
            jsonWriter.name("s").value(superName);
        }
        if (interfaces != null && !interfaces.isEmpty()) {
            jsonWriter.name("i");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, interfaces);
        }
        if (fields != null && !fields.isEmpty()) {
            jsonWriter.name("f");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, fields);
        }
        if (methods != null && !methods.isEmpty()) {
            jsonWriter.name("m");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, methods);
        }
        jsonWriter.name("e").value(fromAndroid);
        jsonWriter.endObject();
    }
}
