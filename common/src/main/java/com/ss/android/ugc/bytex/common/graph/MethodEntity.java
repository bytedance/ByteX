package com.ss.android.ugc.bytex.common.graph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodEntity extends MemberEntity {
    /**
     * sometime exceptions will be empty even it contains exceptions
     */
    public final List<String> exceptions;

    public MethodEntity(int access, String className, String name, String desc) {
        this(access, className, name, desc, null);
    }

    public MethodEntity(int access, String className, String name, String desc, String[] exceptions) {
        super(access, className, name, desc);
        this.exceptions = exceptions == null ? Collections.emptyList() : Arrays.asList(exceptions);
    }

    @Override
    public MemberType type() {
        return MemberType.METHOD;
    }

}
