package com.ss.android.ugc.bytex.getter_setter_inline;

public class ShouldSkipInlineException extends RuntimeException {
    public String reason;

    public ShouldSkipInlineException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
