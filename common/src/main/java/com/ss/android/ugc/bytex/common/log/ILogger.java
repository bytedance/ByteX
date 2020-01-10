
package com.ss.android.ugc.bytex.common.log;

import com.ss.android.ugc.bytex.common.log.Impl.SystemLoggerImpl;

public interface ILogger {
    ILogger DEFAULT = new SystemLoggerImpl();

    void setTag(String tag);

    void d(String msg);

    void d(String tag, String msg);

    void i(String msg);

    void i(String tag, String msg);

    void w(String msg);

    void w(String tag, String msg);

    void w(String msg, Throwable t);

    void w(String tag, String msg, Throwable t);

    void e(String msg);

    void e(String tag, String msg);

    void e(String msg, Throwable t);

    void e(String tag, String msg, Throwable t);
}
