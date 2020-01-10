package com.ss.android.ugc.bytex.transformer.processor;

import java.io.IOException;

/**
 * Created by tlh on 2018/8/21.
 */

public class BackupFileProcessor implements FileProcessor {
    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        return new Output(input.getFileData());
    }
}
