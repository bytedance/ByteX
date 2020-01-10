package com.ss.android.ugc.bytex.transformer.processor;

import com.ss.android.ugc.bytex.transformer.cache.FileData;

import java.io.IOException;

/**
 * Created by tanlehua on 2019/4/27.
 */
public class CommonFileProcessor implements FileProcessor {
    private FileHandler handler;

    private CommonFileProcessor(FileHandler handler) {
        this.handler = handler;
    }


    public static CommonFileProcessor newInstance(FileHandler handler) {
        return new CommonFileProcessor(handler);
    }

    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        FileData fileData = input.getFileData();
        handler.handle(fileData);
        return chain.proceed(input);
    }
}
