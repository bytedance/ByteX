package com.ss.android.ugc.bytex.transformer.processor;

import com.ss.android.ugc.bytex.transformer.cache.FileData;

import java.io.IOException;

/**
 * Created by tlh on 2018/8/22.
 */

public final class ClassFileProcessor implements FileProcessor {
    private FileHandler handler;

    private ClassFileProcessor(FileHandler handler) {
        this.handler = handler;
    }

    public static ClassFileProcessor newInstance(FileHandler transformer) {
        return new ClassFileProcessor(transformer);
    }

    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        FileData fileData = input.getFileData();
        if (fileData.getRelativePath().endsWith(".class")) {
            handler.handle(fileData);
        }
        return chain.proceed(input);
    }
}
