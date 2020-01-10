package com.ss.android.ugc.bytex.shrinkR.exception;

/**
 * Created by tanlehua on 2019/4/18.
 */
public class NotFoundRField {
    public String className;
    public String methodName;
    public String owner;
    public String name;

    public NotFoundRField(String className, String methodName, String owner, String name) {
        this.className = className;
        this.methodName = methodName;
        this.owner = owner;
        this.name = name;
    }
}
