package com.ss.android.ugc.bytex.common.graph;

public class FieldEntity extends MemberEntity {
    public FieldEntity(int access, String className, String name, String desc) {
        super(access, className, name, desc);
    }

    public FieldEntity(int access, String className, String name, String desc, String signature) {
        super(access, className, name, desc, signature);
    }

    @Override
    public MemberType type() {
        return MemberType.FIELD;
    }

}