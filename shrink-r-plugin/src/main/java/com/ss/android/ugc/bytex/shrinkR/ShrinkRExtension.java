package com.ss.android.ugc.bytex.shrinkR;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.List;

public class ShrinkRExtension extends BaseExtension {

    private boolean compatRFileAssignInherit = false;

    private List<String> keepList;

    public List<String> getKeepList() {
        return keepList;
    }

    public void setKeepList(List<String> keepList) {
        this.keepList = keepList;
    }

    @Override
    public String getName() {
        return "shrinkR";
    }

    private String checkMode = "normal";

    public String getCheckMode() {
        return checkMode;
    }

    public void checkMode(String checkMode) {
        this.checkMode = checkMode;
    }

    public boolean isStrictCheckMode() {
        return "strict".equals(checkMode);
    }

    public boolean isCompatRFileAssignInherit() {
        return compatRFileAssignInherit;
    }

    public void setCompatRFileAssignInherit(boolean compatRFileAssignInherit) {
        this.compatRFileAssignInherit = compatRFileAssignInherit;
    }
}
