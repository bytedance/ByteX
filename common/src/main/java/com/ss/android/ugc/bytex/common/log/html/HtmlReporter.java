package com.ss.android.ugc.bytex.common.log.html;

import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.utils.CalendarUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生成ByteX产生的日志的HTML文件的
 */
public class HtmlReporter {
    private static final String TAG = "HtmlReporter";
    private static final String CSS = ".DEBUG { color: black; } .INFO { color: black; } .ERROR { color: red; } .WARNING { color: orange; } table { border: 1px solid #e3e6e8; border-collapse: collapse; display: table; } th { background-color: #189AD6; color: #fff; } .side-bar { width: 100px; position: fixed; bottom:100px; right: 100px; z-index: 100; } .side-bar a { width: 66px; height: 66px; display: inline-block; background-color: #ddd; margin-outside: 100px; } .side-bar .icon-chat {background-position: 0 -130px;position: relative;} .side-bar a:hover { background-color: #669fdd; }";
    private static HtmlReporter sInstance;

    private HtmlReporter() {
    }

    public static HtmlReporter getInstance() {
        if (sInstance == null) {
            synchronized (HtmlReporter.class) {
                if (sInstance == null) {
                    sInstance = new HtmlReporter();
                }
            }
        }
        return sInstance;
    }


    private String htmlFileDir = null;
    private String title;
    private long startTime;
    private String appPackageName;
    private String versionName;
    private String versionCode;
    private final List<HtmlFragmentProvider> htmlFragmentProviders = new ArrayList<>();

    /**
     * register a HtmlFragmentProvider,which will be use to create a piece of HTML code when {@link #createHtmlReporter} is called
     *
     * @param htmlFragmentProvider HtmlFragmentProvider
     */
    public synchronized void registerHtmlFragment(HtmlFragmentProvider htmlFragmentProvider) {
        if (htmlFragmentProvider == null || htmlFragmentProviders.contains(htmlFragmentProvider)) {
            return;
        }
        htmlFragmentProviders.add(htmlFragmentProvider);
    }

    public synchronized void init(String htmlFileDir, String title, String appPackageName, String versionName, String versionCode) {
        for (HtmlFragmentProvider htmlFragmentProvider : htmlFragmentProviders) {
            htmlFragmentProvider.reset();
        }
        this.htmlFileDir = htmlFileDir;
        this.title = title;
        this.startTime = System.currentTimeMillis();
        this.appPackageName = appPackageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
    }

    public synchronized void reset() {
        htmlFragmentProviders.clear();
    }

    /**
     * create a html file which use all registered HtmlFragmentProvider，the file path will be:<br/>
     * htmlFileDir+appPackageName-versionName.versionCode-yyyy-MM-dd_HH-mm-ss_SSS'.html<br/>
     *
     * @return absolutePath
     */
    public String createHtmlReporter(String transformName) {
        File htmlDir = new File(htmlFileDir);
        if (!htmlDir.exists()) {
            htmlDir.mkdirs();
        }
        String fileName = "ByteX_report_" + transformName + ".html";
        File htmlFile = new File(htmlDir, fileName);
        Writer writer = null;
        try {
            if (htmlFile.exists()) {
                htmlFile.delete();
            }
            htmlFile.createNewFile();
            writer = new BufferedWriter(new FileWriter(htmlFile));
            appendHtml(writer);
            writer.flush();
            writer.close();
            return htmlFile.getAbsolutePath();
        } catch (IOException e) {
            LevelLog.sDefaultLogger.e(TAG, e.getMessage(), e);
            if (htmlFile.exists()) {
                htmlFile.delete();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }

    private synchronized void appendHtml(Appendable appendable) throws IOException {
        //构建头部
        appendable.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>").append("<meta charset=\"utf-8\">")
                .append("<style type=\"text/css\">")
                .append(CSS)
                .append("</style>")
                .append("<title>").append(title).append("</title>")
                .append("</head>")
                .append("<body>")
                .append("<h1>").append(title).append("</h1>")
                .append("Transform Start Time: ").append(CalendarUtils.getDateAndTimeString(startTime, true)).append("<br>")
                .append("Report Generated Time: ").append(CalendarUtils.getDateAndTimeString(System.currentTimeMillis(), true)).append("<br>")
                .append("App Package Name: ").append(appPackageName).append("<br>")
                .append("App Version Name: ").append(versionName).append("<br>")
                .append("App Version Code: ").append(versionCode).append("<br>")
                .append(String.format("Total modules: %s: ", String.valueOf(htmlFragmentProviders.size()))).append("<br>");

        //构建主体日志
        appendable.append("<h2>").append(" Transform or check result: ").append("</h2>").append("<ul>\n");
        for (HtmlFragmentProvider provider : htmlFragmentProviders) {
            provider.provideHtmlCode(appendable);
        }
        appendable.append("</ul>");

        appendable.append("<div class=\"side-bar\">\n" +
                "    <a href=\"#\" class=\"icon-chat\">Back To Top</a>\n" +
                "</div>");
        appendable.append("</body>").append("</html>");
    }
}
