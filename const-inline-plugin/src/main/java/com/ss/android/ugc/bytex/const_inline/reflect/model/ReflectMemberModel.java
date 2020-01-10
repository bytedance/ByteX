package com.ss.android.ugc.bytex.const_inline.reflect.model;

/**
 * Created by yangzhiqian on 2019/4/21<br/>
 * Desc:
 */
public abstract class ReflectMemberModel extends ReflectModel {
    public final String memberName;
    public final boolean isDeclared;

    public ReflectMemberModel(String memberName, boolean isDeclared) {
        this(null, memberName, isDeclared);
    }

    public ReflectMemberModel(String owner, String memberName, boolean isDeclared) {
        super(owner);
        this.memberName = memberName;
        this.isDeclared = isDeclared;
    }
}
