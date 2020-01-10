package com.ss.android.ugc.bytex.transformer.processor;

import java.io.IOException;

public interface FileProcessor {

    Output process(Chain chain) throws IOException;

    interface Chain {
        Input input();

        Output proceed(Input input) throws IOException;
    }
}