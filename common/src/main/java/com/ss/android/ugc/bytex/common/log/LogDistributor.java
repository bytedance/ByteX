package com.ss.android.ugc.bytex.common.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * log分发，一个log分发到原来的多个log中
 */
public class LogDistributor implements ILogger {
    private final List<ILogger> loggers = new ArrayList<>();

    public void addLogger(ILogger logger) {
        if (logger == null) {
            return;
        }
        loggers.add(logger);
    }

    public void removeLogger(ILogger logger){
        loggers.remove(logger);
    }

    public List<ILogger> getLoggers(){
        return new ArrayList<>(loggers);
    }

    @Override
    public void setTag(String tag) {
        loggers.forEach((it) -> it.setTag(tag));
    }

    @Override
    public void d(String msg) {
        loggers.forEach((it) -> it.d(msg));
    }

    @Override
    public void d(String tag, String msg) {
        loggers.forEach((it) -> it.d(tag, msg));
    }

    @Override
    public void i(String msg) {
        loggers.forEach((it) -> it.i(msg));
    }

    @Override
    public void i(String tag, String msg) {
        loggers.forEach((it) -> it.i(tag, msg));
    }

    @Override
    public void w(String msg) {
        loggers.forEach((it) -> it.w(msg));
    }

    @Override
    public void w(String tag, String msg) {
        loggers.forEach((it) -> it.w(tag, msg));
    }

    @Override
    public void w(String msg, Throwable t) {
        loggers.forEach((it) -> it.w(msg, t));
    }

    @Override
    public void w(String tag, String msg, Throwable t) {
        loggers.forEach((it) -> it.w(tag, msg, t));
    }

    @Override
    public void e(String msg) {
        loggers.forEach((it) -> it.e(msg));
    }

    @Override
    public void e(String tag, String msg) {
        loggers.forEach((it) -> it.e(tag, msg));
    }

    @Override
    public void e(String msg, Throwable t) {
        loggers.forEach((it) -> it.e(msg, t));
    }

    @Override
    public void e(String tag, String msg, Throwable t) {
        loggers.forEach((it) -> it.e(tag, msg, t));
    }
}
