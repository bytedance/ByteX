package com.ss.android.ugc.bytex.transformer.processor;

import com.ss.android.ugc.bytex.transformer.cache.FileData;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Created by yangzhiqian on 2020-03-06<br/>
 * Desc:按照不同的过程过滤掉不需要的FileData
 */
public class FilterFileProcessor implements FileProcessor {
    private final Predicate<FileData> predicate;

    public FilterFileProcessor(Predicate<FileData> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        if (predicate.test(input.getFileData())) {
            return chain.proceed(input);
        } else {
            return new Output(input.getFileData());
        }
    }
}
