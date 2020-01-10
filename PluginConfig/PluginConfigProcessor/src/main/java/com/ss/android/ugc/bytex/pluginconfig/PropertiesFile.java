package com.ss.android.ugc.bytex.pluginconfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by tanlehua on 2019-05-01.
 */
class PropertiesFile {

    static String getPath(String pluginId) {
        return String.format("META-INF/gradle-plugins/%s.properties", pluginId);
    }

    static void write(String implement, OutputStream output)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, UTF_8));
        writer.write(String.format("implementation-class=%s", implement));
        writer.flush();
    }
}
