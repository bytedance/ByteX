package com.ss.android.ugc.bytex.common.log;


public class LevelLog implements ILogger {
    public static ILogger sDefaultLogger = new LevelLog();
    private ILogger logger;
    private Level level = Level.INFO;

    public LevelLog() {
        this(ILogger.DEFAULT);
    }

    public LevelLog(ILogger logger) {
        setImpl(logger);
    }

    public void setLevel(Level l) {
        level = l;
    }

    public void setImpl(ILogger l) {
        logger = l;
    }

    public ILogger getImpl() {
        return logger;
    }

    @Override
    public void setTag(String tag) {
        this.logger.setTag(tag);
    }

    @Override
    public void d(String msg) {
        if (level.compareTo(Level.DEBUG) <= 0) {
            logger.d(msg);
        }
    }

    @Override
    public void d(String tag, String msg) {
        if (level.compareTo(Level.DEBUG) <= 0) {
            logger.d(tag, msg);
        }
    }

    @Override
    public void i(String msg) {
        if (level.compareTo(Level.INFO) <= 0) {
            logger.i(msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        if (level.compareTo(Level.INFO) <= 0) {
            logger.i(tag, msg);
        }
    }


    @Override
    public void w(String msg) {
        if (level.compareTo(Level.WARN) <= 0) {
            logger.w(msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        if (level.compareTo(Level.WARN) <= 0) {
            logger.w(tag, msg);
        }
    }

    @Override
    public void w(String msg, Throwable t) {
        if (level.compareTo(Level.WARN) <= 0) {
            logger.w(msg, t);
        }
    }

    @Override
    public void w(String tag, String msg, Throwable t) {
        if (level.compareTo(Level.WARN) <= 0) {
            logger.w(tag, msg, t);
        }
    }


    @Override
    public void e(String msg) {
        if (level.compareTo(Level.ERROR) <= 0) {
            logger.e(msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        if (level.compareTo(Level.ERROR) <= 0) {
            logger.e(tag, msg);
        }
    }

    @Override
    public void e(String msg, Throwable t) {
        if (level.compareTo(Level.ERROR) <= 0) {
            logger.e(msg, t);
        }
    }

    @Override
    public void e(String tag, String msg, Throwable t) {
        if (level.compareTo(Level.ERROR) <= 0) {
            logger.e(tag, msg, t);
        }
    }

    public enum Level {
        DEBUG("DEBUG"), INFO("INFO"), WARN("WARN"), ERROR("ERROR");
        String value;

        Level(String value) {
            this.value = value;
        }
    }
}
