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
import java.util.function.Supplier;

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
        if (logFile != null) {
            logFile.delete();
            logFile = null;
        }
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
        if (logger == null || logFile == null || !logFile.exists()) {
            synchronized (this) {
                if (logger == null || logFile == null || !logFile.exists()) {
                    logFile = new File(String.join(File.separator, buildDir().getAbsolutePath(), extension.getLogFile()));
                    logFile.delete();
                    Supplier<HtmlLoggerImpl> htmlLoggerSupplier = () -> {
                        if (BooleanProperty.ENABLE_HTML_LOG.value()) {
                            return new HtmlLoggerImpl(extension.getName());
                        } else {
                            return null;
                        }
                    };
                    HtmlLoggerImpl htmlLogger = logger == null ? htmlLoggerSupplier.get() :
                            ((LogDistributor) ((LevelLog) logger).getImpl()).getLoggers()
                                    .stream()
                                    .filter(iLogger -> iLogger instanceof HtmlLoggerImpl)
                                    .map(iLogger -> (HtmlLoggerImpl) iLogger)
                                    .findFirst()
                                    .orElseGet(htmlLoggerSupplier);
                    ILogger fileLogger;
                    try {
                        fileLogger = FileLoggerImpl.of(logFile.getAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException("can not create log file", e);
                    }
                    LogDistributor logDistributor = new LogDistributor();
                    logDistributor.addLogger(fileLogger);
                    if (htmlLogger != null) {
                        htmlLogger.acceptAllCachedLog((logLevel, tag, msg, throwable) -> {
                            if (logLevel == LogLevel.DEBUG) {
                                fileLogger.d(tag, msg);
                            } else if (logLevel == LogLevel.INFO) {
                                fileLogger.i(tag, msg);
                            } else if (logLevel == LogLevel.WARN) {
                                fileLogger.w(tag, msg, throwable);
                            } else if (logLevel == LogLevel.ERROR) {
                                fileLogger.e(tag, msg, throwable);
                            }
                        });
                        logDistributor.addLogger(htmlLogger);
                        HtmlReporter.getInstance().registerHtmlFragment(htmlLogger);
                    }
                    LevelLog levelLog = new LevelLog(logDistributor);
                    levelLog.setLevel(extension.getLogLevel());
                    levelLog.setTag(extension.getName());
                    logger = levelLog;
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
