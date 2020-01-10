package com.ss.android.ugc.bytex.common.log.Impl;


import com.ss.android.ugc.bytex.common.log.ILogger;

import org.gradle.api.logging.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;


public abstract class BaseLogger implements ILogger {
    private static final String DEFAULT_TAG = "ByteX";
    private String tag = DEFAULT_TAG;

    @Override
    public void setTag(String tag) {
        this.tag = tag == null || "".equals(tag) ? DEFAULT_TAG : tag;
    }

    @Override
    public void d(String msg) {
        d(this.tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        write(LogLevel.DEBUG, tag, msg, null);
    }

    @Override
    public void i(String msg) {
        i(this.tag, msg);
    }

    @Override
    public void i(String tag, String msg) {
        write(LogLevel.INFO, tag, msg, null);
    }

    @Override
    public void w(String msg) {
        w(this.tag, msg);
    }

    @Override
    public void w(String tag, String msg) {
        w(tag, msg, null);
    }

    @Override
    public void w(String msg, Throwable t) {
        w(this.tag, msg, t);
    }

    @Override
    public void w(String tag, String msg, Throwable t) {
        write(LogLevel.WARN, tag, msg, t);
    }

    @Override
    public void e(String msg) {
        e(this.tag, msg);
    }

    @Override
    public void e(String tag, String msg) {
        e(tag, msg, null);
    }

    @Override
    public void e(String msg, Throwable t) {
        e(tag, msg, t);
    }

    @Override
    public void e(String tag, String msg, Throwable t) {
        write(LogLevel.ERROR, tag, msg, t);
    }

    protected abstract void write(LogLevel level, String prefix, String msg, Throwable t);

    static String stackToString(Throwable t) {
        StringWriter sw = new StringWriter(128);
        PrintWriter ps = new PrintWriter(sw);
        t.printStackTrace(ps);
        ps.flush();
        return sw.toString();
    }
}
