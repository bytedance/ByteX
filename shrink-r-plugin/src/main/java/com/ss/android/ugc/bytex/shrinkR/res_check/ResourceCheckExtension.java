package com.ss.android.ugc.bytex.shrinkR.res_check;

import java.util.Collections;
import java.util.List;

public class ResourceCheckExtension {
    private boolean enable;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    private List<String> keepRes;

    public List<String> getKeepRes() {
        if (keepRes == null) {
            keepRes = Collections.emptyList();
        }
        return keepRes;
    }

    public void setKeepRes(List<String> keepRes) {
        this.keepRes = keepRes;
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
