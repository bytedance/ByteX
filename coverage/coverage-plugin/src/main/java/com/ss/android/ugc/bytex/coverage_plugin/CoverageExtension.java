package com.ss.android.ugc.bytex.coverage_plugin;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.List;

/**
 * Created by jiangzilai on 2019-07-15.
 */
public class CoverageExtension extends BaseExtension {

    private List<String> whiteList;
    private boolean clInitOnly = true;

    public boolean isClInitOnly() {
        return clInitOnly;
    }

    public void setClInitOnly(boolean clInitOnly) {
        this.clInitOnly = clInitOnly;
    }

    public void setWhiteList(List<String> list) {
        whiteList = list;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    @Override
    public String getName() {
        return "CoveragePlugin";
    }
}
