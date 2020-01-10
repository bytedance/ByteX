package com.ss.android.ugc.bytex.transformer.processor;

import com.android.build.api.transform.QualifiedContent;
import com.ss.android.ugc.bytex.transformer.cache.FileData;

/**
 * Created by tlh on 2018/8/21.
 */

public class Input {
    public QualifiedContent content;
    private final FileData fileData;

    public Input(QualifiedContent content, FileData fileData) {
        this.content = content;
        this.fileData = fileData;
    }

    public FileData getFileData() {
        return fileData;
    }
}
