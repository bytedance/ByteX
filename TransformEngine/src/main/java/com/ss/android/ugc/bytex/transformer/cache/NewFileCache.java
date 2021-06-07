package com.ss.android.ugc.bytex.transformer.cache;

import com.ss.android.ugc.bytex.transformer.TransformContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.ObservableEmitter;

public class NewFileCache extends DirCache {
    private final String affinity;

    public NewFileCache(TransformContext context, String affinity) {
        this(new ArrayList<>(), affinity, context);
    }

    public NewFileCache(List<FileData> newFiles, String affinity, TransformContext context) {
        super(new File(affinity), getOutput(context, affinity), context);
        this.files = Collections.synchronizedList(newFiles);
        hasRead = true;
        this.affinity = affinity;
    }

    @Override
    protected List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException {
        return Collections.unmodifiableList(files);
    }

    @Override
    public void skip() throws IOException {
        transformOutput();
    }

    @Override
    public List<FileData> getChangedFiles() {
        if (context.isIncremental()) {
            return Collections.unmodifiableList(files);
        } else {
            return Collections.emptyList();
        }
    }

    public void addFile(FileData file) {
        if (isHasWritten()) {
            throw new RuntimeException("can not add file after FileCache has been outputed");
        }
        files.add(file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewFileCache that = (NewFileCache) o;

        return affinity != null ? affinity.equals(that.affinity) : that.affinity == null;
    }

    @Override
    public int hashCode() {
        return affinity != null ? affinity.hashCode() : 0;
    }

    private static File getOutput(TransformContext context, String affinity) {
        try {
            return context.getOutputDir(affinity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
