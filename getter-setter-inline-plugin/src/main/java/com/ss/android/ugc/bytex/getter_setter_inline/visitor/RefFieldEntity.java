package com.ss.android.ugc.bytex.getter_setter_inline.visitor;

import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.RefMemberEntity;

public class RefFieldEntity extends RefMemberEntity<FieldEntity> {
    public RefFieldEntity(FieldEntity entity) {
        super(entity);
    }

    public FieldEntity origin() {
        return origin;
    }


}
