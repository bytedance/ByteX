package com.ss.android.ugc.bytex.shrinkR.res_check;

import java.util.List;

public class AssetsCheckExtension {
    private boolean enable;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    private List<String> keepBySuffix;

    public List<String> getKeepBySuffix() {
        return keepBySuffix;
    }

    public void setKeepBySuffix(List<String> keepBySuffix) {
        this.keepBySuffix = keepBySuffix;
    }

    private List<String> keepAssets;

    public List<String> getKeepAssets() {
        return keepAssets;
    }

    public void setKeepAssets(List<String> keepAssets) {
        this.keepAssets = keepAssets;
    }

}
