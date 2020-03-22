package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public abstract class FileCache implements Serializable {
    protected QualifiedContent content;
    protected TransformContext context;
    protected List<FileData> files;

    public FileCache(QualifiedContent content, TransformContext context) {
        this.content = content;
        this.context = context;
    }

    public void forEach(Consumer<FileData> visitor) {
        parallelForEach(false, visitor);
    }

    public void parallelForEach(boolean parallel, Consumer<FileData> visitor) {
        stream(parallel).subscribe(visitor, throwable -> {
            throw new RuntimeException(throwable);
        });
    }

    public final Observable<FileData> stream() {
        return stream(false);
    }

    public final Observable<FileData> stream(boolean tryParallel) {
        return Observable.create(emitter -> {
            if (files == null) {
                synchronized (this) {
                    if (files == null) {
                        files = resolve(emitter);
                    } else {
                        if (tryParallel) {
                            files.parallelStream().forEach(emitter::onNext);
                        } else {
                            files.forEach(emitter::onNext);
                        }
                    }
                }
            } else {
                if (tryParallel) {
                    files.parallelStream().forEach(emitter::onNext);
                } else {
                    files.forEach(emitter::onNext);
                }
            }
            emitter.onComplete();
        });
    }

    public final void transformOutput() throws IOException {
        transformOutput(null);
    }

    public abstract void transformOutput(Consumer<FileData> visitor) throws IOException;

    protected abstract List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException;

    public abstract void skip() throws IOException;

    public QualifiedContent getContent() {
        return content;
    }

    public List<FileData> getChangedFiles() {
        if (context.isIncremental()) {
            return stream().filter(fileData -> fileData.getStatus() != Status.NOTCHANGED).toList().blockingGet();
        } else return Collections.emptyList();
    }

    public File getFile() {
        if (content != null) {
            return content.getFile();
        }
        return null;
    }

    public boolean containsFileData(String relativePath) {
        return stream().filter(fileData -> fileData.getStatus() != Status.REMOVED && fileData.getRelativePath().equals(relativePath)).firstElement().blockingGet() != null;
    }
}
