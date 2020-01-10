package com.ss.android.ugc.bytex.refercheck;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.List;

public class ReferCheckExtension extends BaseExtension {
    private List<String> whiteList;
    private boolean strictMode;
    private boolean moreErrorInfo;
    private String owner;
    private List<String> logMethods;

    public void setWhiteList(List<String> list) {
        whiteList = list;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void strictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public boolean moreErrorInfo() {
        return moreErrorInfo;
    }

    public void setMoreErrorInfo(boolean moreErrorInfo) {
        this.moreErrorInfo = moreErrorInfo;
    }

    public String getOwner() {
        return owner;
    }

    public void owner(String owner) {
        this.owner = owner;
    }

    public List<String> getLogMethods() {
        return logMethods;
    }

    public void setLogMethods(List<String> logMethods) {
        this.logMethods = logMethods;
    }

    @Override
    public String getName() {
        return "refer_check";
    }
}
