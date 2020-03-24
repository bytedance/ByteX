package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ss.android.ugc.bytex.common.graph.cache.GraphTypeAdapterFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodEntity extends MemberEntity {
    /**
     * sometime exceptions will be empty even it contains exceptions
     */
    public List<String> exceptions;

    public MethodEntity(int access, String className, String name, String desc) {
        this(access, className, name, desc, null);
    }

    public MethodEntity(int access, String className, String name, String desc, String[] exceptions) {
        super(access, className, name, desc);
        this.exceptions = exceptions == null ? Collections.emptyList() : Arrays.asList(exceptions);
    }

    @Override
    public MemberType type() {
        return MemberType.METHOD;
    }

    @Override
    public void read(JsonReader jsonReader, Gson gson) throws IOException {
        if (this.getClass() != MethodEntity.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "access":
                case "a":
                    this.access = jsonReader.nextInt();
                    break;
                case "className":
                case "c":
                    this.className = jsonReader.nextString();
                    break;
                case "name":
                case "n":
                    this.name = jsonReader.nextString();
                    break;
                case "desc":
                case "d":
                    this.desc = jsonReader.nextString();
                    break;
                case "signature":
                case "s":
                    this.signature = jsonReader.nextString();
                    break;
                case "exceptions":
                case "e":
                    this.exceptions = GraphTypeAdapterFactory.readList(jsonReader, gson, String.class);
                    break;
                default:
                    throw new RuntimeException("unsupport json name:" + name);
            }
        }
        jsonReader.endObject();
    }

    @Override
    public void write(JsonWriter jsonWriter, Gson gson) throws IOException {
        if (this.getClass() != MethodEntity.class) {
            throw new IllegalStateException("Jsonable Not Supported");
        }
        jsonWriter.beginObject();

        jsonWriter.name("a").value(access);
        if (className != null) {
            jsonWriter.name("c").value(className);
        }
        if (name != null) {
            jsonWriter.name("n").value(name);
        }
        if (desc != null) {
            jsonWriter.name("d").value(desc);
        }
        if (signature != null) {
            jsonWriter.name("s").value(signature);
        }
        if (exceptions != null && !exceptions.isEmpty()) {
            jsonWriter.name("e");
            GraphTypeAdapterFactory.writeList(jsonWriter, gson, exceptions);
        }
        jsonWriter.endObject();
    }

}
