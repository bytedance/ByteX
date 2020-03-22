package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class FieldEntity extends MemberEntity {
    public FieldEntity(int access, String className, String name, String desc) {
        super(access, className, name, desc);
    }

    public FieldEntity(int access, String className, String name, String desc, String signature) {
        super(access, className, name, desc, signature);
    }

    @Override
    public MemberType type() {
        return MemberType.FIELD;
    }

    @Override
    public void read(JsonReader jsonReader, Gson gson) throws IOException {
        if (this.getClass() != FieldEntity.class) {
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
                default:
                    throw new RuntimeException("unsupport json name:" + name);
            }
        }
        jsonReader.endObject();
    }

    @Override
    public void write(JsonWriter jsonWriter, Gson gson) throws IOException {
        if (this.getClass() != FieldEntity.class) {
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
        jsonWriter.endObject();
    }

}