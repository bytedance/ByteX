package com.ss.android.ugc.bytex.common.log.Impl;


import com.google.common.io.Files;

import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FileLoggerImpl extends BaseLogger {

    public static FileLoggerImpl of(String fileName) throws IOException {
        File logFile = new File(fileName);
        if (!logFile.exists()) {
            Files.createParentDirs(logFile);
        }
        PrintWriter pr = new PrintWriter(new FileOutputStream(fileName), true);
        return new FileLoggerImpl(pr);
    }

    private PrintWriter pr;

    private FileLoggerImpl(PrintWriter pr) {
        this.pr = pr;
    }

    @Override
    protected void write(LogLevel level, String prefix, String msg, Throwable t) {
        pr.println(String.format("%s [%-10s] %s", level.name(), prefix, msg));
        if (t != null) {
            t.printStackTrace(pr);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (pr != null) {
            pr.flush();
            pr.close();
            pr = null;
        }
    }
}
