package com.ss.android.ugc.bytex.common.log.Impl;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class SystemLoggerImpl extends BaseLogger {

    private final Logger logger = Logging.getLogger("lancet");

    @Override
    protected synchronized void write(LogLevel level, String prefix, String msg, Throwable t) {
        if (t != null) {
            logger.log(level, String.format("[%-10s] %s", prefix, msg), t);
        } else {
            logger.log(level, String.format("[%-10s] %s", prefix, msg));
        }
    }
}
