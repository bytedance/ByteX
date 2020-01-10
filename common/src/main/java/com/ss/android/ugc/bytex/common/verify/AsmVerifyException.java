package com.ss.android.ugc.bytex.common.verify;

/**
 * Created by yangzhiqian on 2019/4/18<br/>
 */
public class AsmVerifyException extends RuntimeException {

    public AsmVerifyException() {
    }

    public AsmVerifyException(String message) {
        super(message);
    }

    public AsmVerifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsmVerifyException(Throwable cause) {
        super(cause);
    }

    public AsmVerifyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
