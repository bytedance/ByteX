package com.ss.android.ugc.bytex.common.graph.cache;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ss.android.ugc.bytex.common.graph.ClassEntity;
import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.InterfaceNode;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangzhiqian on 2020-03-03<br/>
 * Desc:
 */
public final class GraphTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType == InterfaceNode.class) {
            return (TypeAdapter<T>) new InterfaceNodeTypeAdapter(gson);
        } else if (rawType == ClassNode.class) {
            return (TypeAdapter<T>) new ClassNodeTypeAdapter(gson);
        } else if (rawType == ClassEntity.class) {
            return (TypeAdapter<T>) new ClassEntityTypeAdapter(gson);
        } else if (rawType == FieldEntity.class) {
            return (TypeAdapter<T>) new FieldEntityTypeAdapter(gson);
        } else if (rawType == MethodEntity.class) {
            return (TypeAdapter<T>) new MethodEntityTypeAdapter(gson);
        } else {
            return null;
        }
    }


    public static <T> List<T> readList(JsonReader jsonReader, Gson gson, Class<T> t) throws IOException {
        JsonToken peek = jsonReader.peek();
        if (peek == JsonToken.NULL) {
            return null;
        }
        jsonReader.beginArray();
        List<T> r = new LinkedList<>();
        while (jsonReader.peek() != JsonToken.END_ARRAY) {
            if (t == String.class) {
                r.add((T) jsonReader.nextString());
            } else {
                r.add(gson.getAdapter(t).read(jsonReader));
            }
        }
        jsonReader.endArray();
        return r;
    }

    public static <T> void writeList(JsonWriter jsonWriter, Gson gson, List<T> list) throws IOException {
        jsonWriter.beginArray();
        for (T t : list) {
            if (t instanceof String) {
                jsonWriter.value((String) t);
            } else if (t instanceof FieldEntity) {
                gson.getAdapter(FieldEntity.class).write(jsonWriter, (FieldEntity) t);
            } else if (t instanceof MethodEntity) {
                gson.getAdapter(MethodEntity.class).write(jsonWriter, (MethodEntity) t);
            } else if (t instanceof InterfaceNode) {
                gson.getAdapter(InterfaceNode.class).write(jsonWriter, (InterfaceNode) t);
            } else if (t instanceof ClassNode) {
                gson.getAdapter(ClassNode.class).write(jsonWriter, (ClassNode) t);
            } else {
                gson.getAdapter(new TypeToken<T>() {
                }).write(jsonWriter, t);
            }
        }
        jsonWriter.endArray();
    }

    private static abstract class SynchronizedTypeAdapter<T> extends TypeAdapter<T> {
        protected final Gson gson;

        SynchronizedTypeAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public final void write(JsonWriter out, T value) throws IOException {
            synchronized (out) {
                writeInSync(out, value);
            }
        }

        @Override
        public T read(JsonReader in) throws IOException {
            synchronized (in) {
                return readInSync(in);
            }
        }

        protected abstract void writeInSync(JsonWriter out, T value) throws IOException;

        protected abstract T readInSync(JsonReader in) throws IOException;
    }


    public final class InterfaceNodeTypeAdapter extends SynchronizedTypeAdapter<InterfaceNode> {

        InterfaceNodeTypeAdapter(Gson gson) {
            super(gson);
        }

        @Override
        protected void writeInSync(JsonWriter out, InterfaceNode value) throws IOException {
            value.write(out, gson);
        }

        @Override
        protected InterfaceNode readInSync(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                return null;
            }
            InterfaceNode node = new InterfaceNode(null);
            node.read(in, gson);
            return node;
        }
    }

    public final class ClassNodeTypeAdapter extends SynchronizedTypeAdapter<ClassNode> {

        ClassNodeTypeAdapter(Gson gson) {
            super(gson);
        }

        @Override
        protected void writeInSync(JsonWriter out, ClassNode value) throws IOException {
            value.write(out, gson);
        }

        @Override
        protected ClassNode readInSync(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                return null;
            }
            ClassNode node = new ClassNode(null);
            node.read(in, gson);
            return node;
        }
    }


    public final class ClassEntityTypeAdapter extends SynchronizedTypeAdapter<ClassEntity> {

        ClassEntityTypeAdapter(Gson gson) {
            super(gson);
        }

        @Override
        protected void writeInSync(JsonWriter out, ClassEntity value) throws IOException {
            value.write(out, gson);
        }

        @Override
        protected ClassEntity readInSync(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                return null;
            }
            ClassEntity entity = new ClassEntity(null, 0);
            entity.read(in, gson);
            return entity;
        }
    }

    public final class FieldEntityTypeAdapter extends SynchronizedTypeAdapter<FieldEntity> {

        FieldEntityTypeAdapter(Gson gson) {
            super(gson);
        }

        @Override
        protected void writeInSync(JsonWriter out, FieldEntity value) throws IOException {
            value.write(out, gson);
        }

        @Override
        protected FieldEntity readInSync(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                return null;
            }
            FieldEntity entity = new FieldEntity(0, null, null, null);
            entity.read(in, gson);
            return entity;
        }
    }

    public final class MethodEntityTypeAdapter extends SynchronizedTypeAdapter<MethodEntity> {

        MethodEntityTypeAdapter(Gson gson) {
            super(gson);
        }

        @Override
        protected void writeInSync(JsonWriter out, MethodEntity value) throws IOException {
            value.write(out, gson);
        }

        @Override
        protected MethodEntity readInSync(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                return null;
            }
            MethodEntity entity = new MethodEntity(0, null, null, null);
            entity.read(in, gson);
            return entity;
        }
    }
}
