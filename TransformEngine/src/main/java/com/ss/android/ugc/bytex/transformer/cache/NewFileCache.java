package com.ss.android.ugc.bytex.transformer.cache;

import com.google.common.io.Files;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class NewFileCache extends FileCache {
    private final List<FileData> files;
    private final String affinity;

    public NewFileCache(List<FileData> files, String affinity, TransformContext context) {
        super(null, context);
        this.files = files;
        this.affinity = affinity;
    }

    public NewFileCache(TransformContext context, String affinity) {
        this(new ArrayList<>(), affinity, context);
    }

    @Override
    public void transformOutput(Consumer<FileData> visitor) throws IOException {
        File outputDir = context.getOutputDir(affinity);
        forEach(fileData -> {
            try {
                if (visitor != null) visitor.accept(fileData);
                byte[] bytes = fileData.getBytes();
                if (bytes != null && bytes.length > 0) {
                    File target = TransformContext.getOutputTarget(outputDir, fileData.getRelativePath());
                    Files.write(bytes, target);
                }
                for (FileData attachment : fileData.getAttachment()) {
                    bytes = attachment.getBytes();
                    if (bytes != null && bytes.length > 0) {
                        File target = TransformContext.getOutputTarget(outputDir, attachment.getRelativePath());
                        Files.write(bytes, target);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException {
        return new ArrayList<>(files);
    }

    @Override
    public void skip() throws IOException {
        transformOutput();
    }

    @Override
    public List<FileData> getChangedFiles() {
        return files;
    }

    public void addFile(FileData file) {
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
}
