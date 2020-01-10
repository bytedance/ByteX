package com.ss.android.ugc.bytex.serialization_check;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.Collections;
import java.util.List;

public class SerializationCheckExtension extends BaseExtension {
    private List<String> whiteList;

    @Override
    public String getName() {
        return "SerializationCheck";
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    private List<String> onlyCheck;

    public List<String> getOnlyCheck() {
        if (onlyCheck == null) {
            onlyCheck = Collections.emptyList();
        }
        return onlyCheck;
    }

    public void setOnlyCheck(List<String> onlyCheck) {
        this.onlyCheck = onlyCheck;
    }
}
