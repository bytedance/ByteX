package com.ss.android.ugc.bytex.const_inline;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.HashSet;

public class ConstInlineExtension extends BaseExtension {
    /**
     * The classes in the whiteList will skip check
     * Format:ClassName.FieldName.FieldDesc
     * Support pattern matching
     * such as "com/meizu/cloud/*"
     */
    private HashSet<String> whiteList = new HashSet<>();
    private HashSet<String> skipWithAnnotations = new HashSet<>();
    /**
     * Whether to optimize fields which contain runtime annotations
     */
    private boolean skipWithRuntimeAnnotation = true;
    /**
     * Whether to skip possible reflection field optimization based on plugin analysis
     */
    private boolean autoFilterReflectionField = false;
    /**
     * Whether to skip  possible reflection field optimization based on matching strings
     */
    private boolean supposesReflectionWithString = false;

    @Override
    public String getName() {
        return "const_inline";
    }

    public HashSet<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(HashSet<String> whiteList) {
        this.whiteList = whiteList;
    }

    public boolean isAutoFilterReflectionField() {
        return autoFilterReflectionField;
    }

    public void setAutoFilterReflectionField(boolean autoFilterReflectionField) {
        this.autoFilterReflectionField = autoFilterReflectionField;
    }

    public boolean isSupposesReflectionWithString() {
        return supposesReflectionWithString;
    }

    public void setSupposesReflectionWithString(boolean supposesReflectionWithString) {
        this.supposesReflectionWithString = supposesReflectionWithString;
    }

    public HashSet<String> getSkipWithAnnotations() {
        return skipWithAnnotations;
    }

    public void setSkipWithAnnotations(HashSet<String> skipWithAnnotations) {
        this.skipWithAnnotations = skipWithAnnotations;
    }

    public boolean isSkipWithRuntimeAnnotation() {
        return skipWithRuntimeAnnotation;
    }

    public void setSkipWithRuntimeAnnotation(boolean skipWithRuntimeAnnotation) {
        this.skipWithRuntimeAnnotation = skipWithRuntimeAnnotation;
    }
}
