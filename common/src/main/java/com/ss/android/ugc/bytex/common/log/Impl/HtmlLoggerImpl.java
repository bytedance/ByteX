package com.ss.android.ugc.bytex.common.log.Impl;

import com.ss.android.ugc.bytex.common.log.html.HtmlFragmentProvider;
import com.ss.android.ugc.bytex.common.utils.CalendarUtils;

import org.gradle.api.logging.LogLevel;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import kotlin.Triple;

public class HtmlLoggerImpl extends CachedLogger implements HtmlFragmentProvider {
    private String moduleName;

    public HtmlLoggerImpl(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public synchronized void provideHtmlCode(Appendable appendable) throws IOException {
        appendable.append("<li>\n")
                .append(String.format("%s Check Results:E(%s),W(%s),I(%s),D(%s)",
                        moduleName,
                        getSize(LogLevel.ERROR),
                        getSize(LogLevel.WARN),
                        getSize(LogLevel.INFO),
                        getSize(LogLevel.DEBUG)
                ))
                .append("</li>\n");
        getExpandableCodeString(appendable, "Click To Expend for details", this::getTable);
    }

    @Override
    public void reset() {
        clear();
    }

    private void getTable(Appendable appendable) throws IOException {
        appendable.append("<table border=\"1\" cellpadding=\"8\">")
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
            appendLevelLog(appendable, true, "ERROR", "ERROR", error);
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> warn = logs.get(LogLevel.WARN);
        if (warn != null) {
            appendLevelLog(appendable, needAppendModuleName, "WARNING", "WARN", warn);
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> info = logs.get(LogLevel.INFO);
        if (info != null) {
            appendLevelLog(appendable, needAppendModuleName, "INFO", "INFO", info);
            needAppendModuleName = false;
        }
        Map<String, List<Triple<String, Throwable, Long>>> debug = logs.get(LogLevel.DEBUG);
        if (debug != null) {
            appendLevelLog(appendable, needAppendModuleName, "DEBUG", "DEBUG", debug);
        }
        appendable.append("</table>");
    }

    private void appendLevelLog(Appendable appendable, boolean needAppendModuleName, String ssc, String levelName, Map<String, List<Triple<String, Throwable, Long>>> levelLogs) throws IOException {
        for (String key : levelLogs.keySet()) {
            List<Triple<String, Throwable, Long>> value = levelLogs.get(key);
            for (Triple<String, Throwable, Long> triple : value) {
                appendable.append("<tr>");
                if (needAppendModuleName) {
                    appendable.append("<td").append(" rowspan=\"").append(String.valueOf(getTotalRows())).append("\"").append(">").append(moduleName).append("</td>");
                    needAppendModuleName = false;
                }
                appendable.append("<td").append(" class=\"").append(ssc).append("\">").append(CalendarUtils.getDateAndTimeString(triple.getThird(), true)).append("</td>");
                appendable.append("<td").append(" class=\"").append(ssc).append("\">").append(levelName).append("</td>");
                appendable.append("<td").append(" class=\"").append(ssc).append("\">").append(key).append("</td>");
                if (triple.getSecond() != null) {

                    appendable.append("<td").append(" class=\"").append(ssc).append("\">").append("<p>");
                    getExpandableCodeString(appendable, triple.getFirst(), appendable1 -> {
                        StringWriter stackTraceHolder = new StringWriter();
                        triple.getSecond().printStackTrace(new PrintWriter(stackTraceHolder));
                        appendable1.append(stackTraceHolder.toString());
                    });
                    appendable.append("</p></td>");
                } else {
                    appendable.append("<td").append(" class=\"").append(ssc).append("\">").append(triple.getFirst()).append("</td>");
                }
                appendable.append("</tr>");
            }
        }
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

    private static void getExpandableCodeString(Appendable appendable, String title, Func1<Appendable> action) throws IOException {
        appendable.append("<details>\n")
                .append("<summary>")
                .append(title)
                .append("</summary>\n")
                .append("<pre><code>");
        action.apply(appendable);
        appendable.append("</code></pre>\n")
                .append("</details>\n");
    }

    public interface Func4<F, S, T, FO> {
        void apply(F f, S s, T t, FO fo);
    }

    public interface Func1<F> {
        void apply(F f) throws IOException;
    }

}
