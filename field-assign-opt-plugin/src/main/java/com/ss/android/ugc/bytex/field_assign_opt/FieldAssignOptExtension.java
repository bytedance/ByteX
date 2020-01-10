package com.ss.android.ugc.bytex.field_assign_opt;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.HashSet;

public class FieldAssignOptExtension extends BaseExtension {
    /**
     * Whether to remove the line number information<br/>
     * The assignment statement is normally on a single line, and the compiler
     * will generate the corresponding line number attribute<br/>
     */
    private boolean removeLineNumber = true;
    /**
     * The classes in the whiteList will skip optimization
     * Format:ClassName.FieldName
     */
    private HashSet<String> whiteList = new HashSet<>();

    @Override
    public String getName() {
        return "field_assign_opt";
    }

    public boolean isRemoveLineNumber() {
        return removeLineNumber;
    }

    public void setRemoveLineNumber(boolean removeLineNumber) {
        this.removeLineNumber = removeLineNumber;
    }

    public HashSet<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(HashSet<String> whiteList) {
        this.whiteList = whiteList;
    }
}
