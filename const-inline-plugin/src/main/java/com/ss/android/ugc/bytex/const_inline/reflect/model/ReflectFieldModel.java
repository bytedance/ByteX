package com.ss.android.ugc.bytex.const_inline.reflect.model;

/**
 * Created by yangzhiqian on 2019/4/21<br/>
 * Desc:
 */
public class ReflectFieldModel extends ReflectMemberModel {


    public ReflectFieldModel(String memberName, boolean isDeclared) {
        super(memberName, isDeclared);
    }

    public ReflectFieldModel(String owner, String memberName, boolean isDeclared) {
        super(owner, memberName, isDeclared);
    }

    @Override
    public int getReflectType() {
        if (memberName == null) {
            return isDeclared ? REFLECT_TYPE_DECLARED_FIELDS : REFLECT_TYPE_FIELDS;
        } else {
            return isDeclared ? REFLECT_TYPE_DECLARED_FIELD : REFLECT_TYPE_FIELD;
        }
    }
}
