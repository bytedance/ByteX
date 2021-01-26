package com.ss.android.ugc.bytex.refercheck;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.ArrayList;
import java.util.List;

public class ReferCheckExtension extends BaseExtension {
    private List<String> whiteList;
    private boolean strictMode;
    private boolean moreErrorInfo;
    private String owner;
    private boolean printKept = false;
    private List<String> callBlockList = new ArrayList<>();

    public List<String> getCallBlockList() {
        return callBlockList;
    }

    public void setCallBlockList(List<String> callBlockList) {
        this.callBlockList = callBlockList;
    }

    public boolean isCheckInaccessOverrideMethodStrictly() {
        return checkInaccessOverrideMethodStrictly;
    }

    public void setCheckInaccessOverrideMethodStrictly(boolean checkInaccessOverrideMethodStrictly) {
        this.checkInaccessOverrideMethodStrictly = checkInaccessOverrideMethodStrictly;
    }

    private boolean checkInaccessOverrideMethodStrictly = false;

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

    @Override
    public String getName() {
        return "refer_check";
    }

    public boolean isPrintKept() {
        return printKept;
    }

    public void setPrintKept(boolean printKept) {
        this.printKept = printKept;
    }
}
