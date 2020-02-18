package com.ss.android.ugc.bytex.common;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.log.ILogger;
import com.ss.android.ugc.bytex.common.log.Impl.FileLoggerImpl;
import com.ss.android.ugc.bytex.common.log.Impl.HtmlLoggerImpl;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.log.LogDistributor;
import com.ss.android.ugc.bytex.common.log.html.HtmlReporter;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BaseContext<E extends BaseExtension> {
    protected final Project project;
    protected final AppExtension android;
    public final E extension;
    private ILogger logger;
    private Graph classGraph;
    private File logFile;
    private boolean hasInitialized;
    private TransformContext transformContext;

    public BaseContext(Project project, AppExtension android, E extension) {
        this.project = project;
        this.android = android;
        this.extension = extension;
    }

    public void init() {
        if (hasInitialized) {
            return;
        }
        hasInitialized = true;
        //init logger
        getLogger().d("init");
    }

    private String getSdkJarDir() {
        String compileSdkVersion = android.getCompileSdkVersion();
        return String.join(File.separator, android.getSdkDirectory().getAbsolutePath(), "platforms", compileSdkVersion);
    }

    public File buildDir() {
        try {
            return new File(transformContext.byteXBuildDir(), extension.getName());
        } catch (Exception e) {
            return new File(new File(project.getBuildDir(), "ByteX"), extension.getName());
        }
    }

    public File androidJar() throws FileNotFoundException {
        File jar = new File(getSdkJarDir(), "android.jar");
        if (!jar.exists()) {
            throw new FileNotFoundException("Android jar not found!");
        }
        return jar;
    }

    public final ILogger getLogger() {
        if (logger == null) {
            synchronized (this) {
                if (logger == null) {
                    ILogger fileLog;
                    try {
                        String fileName = String.join(File.separator, buildDir().getAbsolutePath(), extension.getLogFile());
                        Files.deleteIfExists(Paths.get(fileName));
                        logFile = new File(fileName);
                        fileLog = FileLoggerImpl.of(fileName);
                    } catch (IOException e) {
                        throw new RuntimeException("can not create log file", e);
                    }
                    LogDistributor logDistributor = new LogDistributor();
                    logDistributor.addLogger(fileLog);
                    if (BooleanProperty.ENABLE_HTML_LOG.value()) {
                        HtmlLoggerImpl htmlLog = new HtmlLoggerImpl(extension.getName());
                        logDistributor.addLogger(htmlLog);
                        HtmlReporter.getInstance().registerHtmlFragment(htmlLog);
                    }
                    LevelLog levelLog = new LevelLog(logDistributor);
                    levelLog.setLevel(extension.getLogLevel());
                    levelLog.setTag(extension.getName());
                    logger = levelLog;
                }
            }
        }
        if (!logFile.exists()) {
            //be deleted? bad case.get cached logs and write to logfile
            synchronized (this) {
                if (!logFile.exists()) {
                    //ignore ClassCastException
                    LogDistributor logDistributor = (LogDistributor) ((LevelLog) logger).getImpl();
                    List<ILogger> loggers = logDistributor.getLoggers();
                    loggers.stream().filter(it -> it instanceof HtmlLoggerImpl).findFirst().ifPresent(hlog -> {
                        final HtmlLoggerImpl htmlLog = (HtmlLoggerImpl) hlog;
                        //lock htmllogger
                        synchronized (htmlLog) {
                            loggers.stream().filter(it -> it instanceof FileLoggerImpl).map(flog -> {
                                try {
                                    ((FileLoggerImpl) flog).redirectLogFile(logFile.getAbsolutePath());
                                    return flog;
                                } catch (IOException e) {
                                    throw new RuntimeException("can not create log file", e);
                                }
                            }).forEach(flog -> {
                                //ignore level
                                htmlLog.acceptAllCachedLog((logLevel, tag, msg, throwable) -> {
                                    if (logLevel == LogLevel.DEBUG) {
                                        flog.d(tag, msg);
                                    } else if (logLevel == LogLevel.INFO) {
                                        flog.i(tag, msg);
                                    } else if (logLevel == LogLevel.WARN) {
                                        flog.w(tag, msg, throwable);
                                    } else if (logLevel == LogLevel.ERROR) {
                                        flog.e(tag, msg, throwable);
                                    }
                                });
                            });
                        }
                    });
                }
            }
        }
        return logger;
    }

    public Graph getClassGraph() {
        return classGraph;
    }

    void setClassGraph(Graph classGraph) {
        this.classGraph = classGraph;
    }

    public TransformContext getTransformContext() {
        return transformContext;
    }

    void setTransformContext(TransformContext transformContext) {
        this.transformContext = transformContext;
    }

    public Project getProject() {
        return project;
    }

    public AppExtension getAndroid() {
        return android;
    }
}
