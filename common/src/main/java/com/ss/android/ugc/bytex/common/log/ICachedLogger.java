
package com.ss.android.ugc.bytex.common.log;

import org.gradle.api.logging.LogLevel;

public interface ICachedLogger extends ILogger {

    void accept(CachedLogVisitor logger);

    void clear();


    interface CachedLogVisitor {
        void visitLog(long logTime, LogLevel level, String prefix, String msg, Throwable t);
    }
}
