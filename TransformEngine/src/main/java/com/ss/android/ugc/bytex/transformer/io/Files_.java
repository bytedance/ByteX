package com.ss.android.ugc.bytex.transformer.io;

import com.google.common.collect.TreeTraverser;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by tanlehua on 2019-04-30.
 */
public class Files_ {
    public static TreeTraverser<File> fileTreeTraverser() {
        return FILE_TREE_TRAVERSER;
    }

    private static final TreeTraverser<File> FILE_TREE_TRAVERSER =
            new TreeTraverser<File>() {
                @Override
                public Iterable<File> children(File file) {
                    // check isDirectory() just because it may be faster than listFiles() on a non-directory
                    if (file.isDirectory()) {
                        File[] files = file.listFiles();
                        if (files != null) {
                            return Collections.unmodifiableList(Arrays.asList(files));
                        }
                    }

                    return Collections.emptyList();
                }

                @Override
                public String toString() {
                    return "Files.fileTreeTraverser()";
                }
            };
}
