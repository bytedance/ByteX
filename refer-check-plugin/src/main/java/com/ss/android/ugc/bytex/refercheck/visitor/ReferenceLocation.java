package com.ss.android.ugc.bytex.refercheck.visitor;

import com.ss.android.ugc.bytex.common.utils.Utils;

public class ReferenceLocation {
    public String clzLoc;
    public String methodLoc;
    public int line;
    public String sourceFile;
    private boolean exist;
    public int access;

    public ReferenceLocation(boolean exist) {
        this.exist = exist;
    }

    public ReferenceLocation(String clzLoc, String methodLoc, int line, boolean exist) {
        this.clzLoc = clzLoc;
        this.methodLoc = methodLoc;
        this.line = line;
        this.exist = exist;
    }

    public boolean isExist() {
        return exist;
    }

    public boolean inaccessible() {
        return access >= 0;
    }

    @Override
    public String toString() {
        return "{" +
                "clzLoc='" + clzLoc + '\'' +
                ", methodLoc='" + methodLoc + '\'' +
                ", line=" + line +
                ", sourceFile='" + sourceFile + '\'' +
                ", exist=" + exist +
                '}';
    }

    public String getSourceFile() {
        if (sourceFile == null) {
            return String.format("%s.java", Utils.resolveClassName(clzLoc).getSecond());
        } else return sourceFile;
    }
}