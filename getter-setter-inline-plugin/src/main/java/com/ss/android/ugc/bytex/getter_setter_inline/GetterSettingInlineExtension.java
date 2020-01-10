package com.ss.android.ugc.bytex.getter_setter_inline;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.Collections;
import java.util.List;

public class GetterSettingInlineExtension extends BaseExtension {
    private List<String> keepList;
    private List<String> keepWithAnnotations;
    private List<String> shouldInline;

    public List<String> getShouldInline() {
        if (shouldInline == null) {
            shouldInline = Collections.emptyList();
        }
        return shouldInline;
    }

    public void setShouldInline(List<String> shouldInline) {
        this.shouldInline = shouldInline;
    }

    public List<String> getKeepWithAnnotations() {
        return keepWithAnnotations;
    }

    public void setKeepWithAnnotations(List<String> keepWithAnnotations) {
        this.keepWithAnnotations = keepWithAnnotations;
    }

    public void setKeepList(List<String> list) {
        keepList = list;
    }

    public List<String> getKeepList() {
        return keepList;
    }

    @Override
    public String getName() {
        return "getter_setter_inline";
    }
}
