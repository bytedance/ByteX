package com.ss.android.ugc.bytex.access_inline;


import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.HashSet;
import java.util.Set;

public class AccessInlineExtension extends BaseExtension {
    private Set<String> whiteList = new HashSet<>();

    public Set<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public String getName() {
        return "access_inline";
    }
}
