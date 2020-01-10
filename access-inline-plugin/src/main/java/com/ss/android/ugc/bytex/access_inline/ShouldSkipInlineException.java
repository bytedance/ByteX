package com.ss.android.ugc.bytex.access_inline;

public class ShouldSkipInlineException extends RuntimeException {
    public String reason;

    public ShouldSkipInlineException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
