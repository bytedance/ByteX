package com.ss.android.ugc.bytex.common;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.log.ICachedLogger;
import com.ss.android.ugc.bytex.common.log.ILogger;
import com.ss.android.ugc.bytex.common.log.Impl.CachedLogger;
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
import java.lang.ref.WeakReference;

public class BaseContext<E extends BaseExtension> {
    protected final Project project;
    protected final AppExtension android;
    public final E extension;
    private ILogger logger;
    private final ICachedLogger cachedLogger = new AdjustableCachedLogger(new WeakReference<>(this));
    private Graph classGraph;
    private TransformContext transformContext;

    public BaseContext(Project project, AppExtension android, E extension) {
        this.project = project;
        this.android = android;
        this.extension = extension;
    }

    public synchronized void init() {
        if (logger != null) {
            return;
        }
        logger = createLogger();
        synchronized (cachedLogger) {
            cachedLogger.accept((logTime, level, prefix, msg, t) -> {
                switch (level) {
                    case DEBUG:
                        logger.d(prefix, msg);
                        break;
                    case INFO:
                        logger.i(prefix, msg);
                        break;
                    case WARN:
                        logger.w(prefix, msg, t);
                        break;
                    case ERROR:
                        logger.e(prefix, msg, t);
                        break;
                    default:
                        throw new IllegalArgumentException(level.toString());
                }
            });
            cachedLogger.clear();
        }
        getLogger().i("init");
    }

    protected ILogger createLogger() {
        File logFile = getLoggerFile();
        logFile.delete();
        ILogger fileLogger;
        try {
            fileLogger = FileLoggerImpl.of(logFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("can not create log file", e);
        }
        LogDistributor logDistributor = new LogDistributor();
        logDistributor.addLogger(fileLogger);
        if (BooleanProperty.ENABLE_HTML_LOG.value()) {
            HtmlLoggerImpl htmlLogger = new HtmlLoggerImpl(extension.getName());
            HtmlReporter.getInstance().registerHtmlFragment(htmlLogger);
            logDistributor.addLogger(htmlLogger);
        }
        LevelLog levelLog = new LevelLog(logDistributor);
        levelLog.setLevel(extension.getLogLevel());
        levelLog.setTag(extension.getName());
        return levelLog;
    }

    protected File getLoggerFile() {
        return new File(new File(transformContext.byteXBuildDir(), extension.getName()), extension.getLogFile());
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
        if (logger != null) {
            return logger;
        }
        return cachedLogger;
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
        if (transformContext == null) {
            this.transformContext = null;
            return;
        }
        if (this.transformContext != null && this.transformContext != transformContext) {
            throw new IllegalStateException("transformContext configured twice");
        }
        this.transformContext = transformContext;
    }

    public Project getProject() {
        return project;
    }

    public AppExtension getAndroid() {
        return android;
    }

    public synchronized void releaseContext() {
        logger = null;
        classGraph = null;
        transformContext = null;
    }

    private static class AdjustableCachedLogger extends CachedLogger {
        private final WeakReference<BaseContext<?>> weakContext;

        AdjustableCachedLogger(WeakReference<BaseContext<?>> weakContext) {
            this.weakContext = weakContext;
        }

        @Override
        protected void write(LogLevel level, String prefix, String msg, Throwable t) {
            BaseContext<?> baseContext = weakContext.get();
            if (baseContext == null) {
                super.write(level, prefix, msg, t);
                return;
            }
            ILogger realLogger = baseContext.logger;
            if (realLogger == null) {
                super.write(level, prefix, msg, t);
                return;
            }
            switch (level) {
                case DEBUG:
                    realLogger.d(prefix, msg);
                    break;
                case INFO:
                    realLogger.i(prefix, msg);
                    break;
                case WARN:
                    realLogger.w(prefix, msg, t);
                    break;
                case ERROR:
                    realLogger.e(prefix, msg, t);
                    break;
                default:
                    throw new IllegalArgumentException(level.toString());
            }
        }
    }
}
