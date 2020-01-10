package com.ss.android.ugc.bytex.common.log.Impl;

import com.ss.android.ugc.bytex.common.log.html.HtmlFragmentProvider;
import com.ss.android.ugc.bytex.common.utils.CalendarUtils;

import org.gradle.api.logging.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import kotlin.Triple;

public class HtmlLoggerImpl extends BaseLogger implements HtmlFragmentProvider {
    private String moduleName;
    private final Map<LogLevel, Map<String, List<Triple<String, Throwable, Long>>>> logs = new LinkedHashMap<>();

    public HtmlLoggerImpl(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    protected synchronized void write(LogLevel level, String prefix, String msg, Throwable t) {
        logs.computeIfAbsent(level, (it) -> new HashMap<>()).computeIfAbsent(prefix, (it) -> new ArrayList<>()).add(new Triple<>(msg, t, System.currentTimeMillis()));
    }

    @Override
    public synchronized String provide() {

        return "<li>\n" +
                String.format("%s Check Results:E(%s),W(%s),I(%s),D(%s)",
                        moduleName,
                        getSize(LogLevel.ERROR),
                        getSize(LogLevel.WARN),
                        getSize(LogLevel.INFO),
                        getSize(LogLevel.DEBUG)
                ) +
                "</li>\n" +
                getExpandableCodeString("Click To Expend for details", getTable());
    }

    @Override
    public void reset() {
        logs.clear();
    }

    public void acceptAllCachedLog(Func4<LogLevel, String, String, ? super Throwable> func4) {
        synchronized (this) {
            logs.keySet().forEach((level) -> {
                logs.get(level).keySet().forEach((tag) -> {
                    logs.get(level).get(tag).forEach((trip) -> {
                        func4.apply(level, tag, trip.getFirst(), trip.getSecond());
                    });
                });
            });
        }
    }

    private String getTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" cellpadding=\"8\">")
                .append("<tr>")
                .append("<th>").append("Module").append("</th>")
                .append("<th>").append("Time").append("</th>")
                .append("<th>").append("Level").append("</th>")
                .append("<th>").append("Tag").append("</th>")
                .append("<th>").append("Info").append("</th>")
                .append("</tr>");

        Map<String, List<Triple<String, Throwable, Long>>> error = logs.get(LogLevel.ERROR);
        boolean needAppendModuleName = true;
        if (error != null) {
            sb.append(getLevelLog(true, "ERROR", "ERROR", error));
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> warn = logs.get(LogLevel.WARN);
        if (warn != null) {
            sb.append(getLevelLog(needAppendModuleName, "WARNING", "WARN", warn));
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> info = logs.get(LogLevel.INFO);
        if (info != null) {
            sb.append(getLevelLog(needAppendModuleName, "INFO", "INFO", info));
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> debug = logs.get(LogLevel.DEBUG);
        if (debug != null) {
            sb.append(getLevelLog(needAppendModuleName, "DEBUG", "DEBUG", debug));
        }
        sb.append("</table>");

        return sb.toString();
    }

    private String getLevelLog(boolean needAppendModuleName, String ssc, String levelName, Map<String, List<Triple<String, Throwable, Long>>> levelLogs) {
        StringBuilder sb = new StringBuilder();
        for (String key : levelLogs.keySet()) {
            List<Triple<String, Throwable, Long>> value = levelLogs.get(key);
            for (Triple<String, Throwable, Long> triple : value) {
                sb.append("<tr>");
                if (needAppendModuleName) {
                    sb.append("<td").append(" rowspan=\"").append(getTotalRows()).append("\"").append(">").append(moduleName).append("</td>");
                    needAppendModuleName = false;
                }
                sb.append("<td").append(" class=\"").append(ssc).append("\">").append(CalendarUtils.getDateAndTimeString(triple.getThird(), true)).append("</td>");
                sb.append("<td").append(" class=\"").append(ssc).append("\">").append(levelName).append("</td>");
                sb.append("<td").append(" class=\"").append(ssc).append("\">").append(key).append("</td>");
                if (triple.getSecond() != null) {
                    StringWriter stackTraceHolder = new StringWriter();
                    triple.getSecond().printStackTrace(new PrintWriter(stackTraceHolder));
                    sb.append("<td").append(" class=\"").append(ssc).append("\">").append("<p>").append(getExpandableCodeString(triple.getFirst(), stackTraceHolder.toString())).append("</p></td>");
                } else {
                    sb.append("<td").append(" class=\"").append(ssc).append("\">").append(triple.getFirst()).append("</td>");
                }
                sb.append("</tr>");
            }
        }
        return sb.toString();
    }

    private int getTotalRows() {
        AtomicInteger size = new AtomicInteger();
        logs.keySet().stream().filter((it) -> it == LogLevel.ERROR || it == LogLevel.WARN || it == LogLevel.INFO || it == LogLevel.DEBUG)
                .flatMap((it) -> logs.get(it).values().stream())
                .forEach((it) -> size.addAndGet(it.size()));
        return size.get();
    }

    private int getSize(LogLevel logLevel) {
        AtomicInteger size = new AtomicInteger();
        logs.keySet().stream().filter((it) -> it == logLevel)
                .flatMap((it) -> logs.get(it).values().stream())
                .forEach((it) -> size.addAndGet(it.size()));
        return size.get();
    }

    private static String getExpandableCodeString(String title, String code) {
        return "<details>\n" +
                "<summary>" +
                title +
                "</summary>\n" +
                "<pre><code>" +
                code +
                "</code></pre>\n" +
                "</details>\n";
    }

    @FunctionalInterface
    public
    interface Func4<F, S, T, FO> {
        void apply(F f, S s, T t, FO fo);
    }
}
