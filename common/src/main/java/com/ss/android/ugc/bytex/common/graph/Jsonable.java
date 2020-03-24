package com.ss.android.ugc.bytex.common.graph;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by yangzhiqian on 2020-03-03<br/>
 * Desc:
 */
public interface Jsonable extends Serializable {
    void read(JsonReader jsonReader, Gson gson) throws IOException;

    void write(JsonWriter jsonWriter, Gson gson) throws IOException;
}
