package com.ss.android.ugc.bytex.sourcefilekiller;

import com.ss.android.ugc.bytex.common.BaseExtension;

public class SourceFileExtension extends BaseExtension {
    private boolean deleteSourceFile;
    private boolean deleteLineNumber;

    public boolean isDeleteSourceFile() {
        return deleteSourceFile;
    }

    public void setDeleteSourceFile(boolean deleteSourceFile) {
        this.deleteSourceFile = deleteSourceFile;
    }

    public boolean isDeleteLineNumber() {
        return deleteLineNumber;
    }

    public void setDeleteLineNumber(boolean deleteLineNumber) {
        this.deleteLineNumber = deleteLineNumber;
    }

    @Override
    public String getName() {
        //在gradle中写插件配置dsl时的名字
        //the name of the plugin to configure dsl in gradle
        return "SourceFile";
    }
}
