package com.ss.android.ugc.bytex.transformer.processor;

import com.ss.android.ugc.bytex.transformer.cache.FileData;

/**
 * Created by tlh on 2018/8/21.
 */

public class Output {
    private final FileData fileData;

    public Output(FileData fileData) {
        this.fileData = fileData;
    }

    public FileData getFileData() {
        return fileData;
    }
}
