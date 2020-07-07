package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.Status;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by tlh on 2018/8/22.
 */

public class FileData implements Serializable {
    private byte[] bytes;
    private LoadFunction bytesCallable;
    private String relativePath;
    private List<FileData> attachment = Collections.emptyList();
    private Status status;

    public FileData(byte[] bytes, String relativePath) {
        this(bytes, relativePath, Status.ADDED);
    }

    public FileData(byte[] bytes, String relativePath, Status status) {
        this.bytes = bytes;
        this.relativePath = relativePath;
        this.status = status;
    }

    public FileData(LoadFunction bytesCallable, String relativePath, Status status) {
        this.bytesCallable = bytesCallable;
        this.bytes = null;
        this.relativePath = relativePath;
        this.status = status;
    }

    public byte[] getBytes() {
        if (bytesCallable != null) {
            synchronized (this) {
                if (bytesCallable != null) {
                    try {
                        setBytes(bytesCallable.load(this), this.status);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return bytes;
    }

    public void delete() {
        setBytes(null);
    }

    public void setBytes(byte[] bytes) {
        this.setBytes(bytes, getBytes() == bytes ? this.status : bytes == null ? Status.REMOVED : Status.CHANGED);
    }

    public void setBytes(byte[] bytes, Status status) {
        this.bytes = bytes;
        this.status = status;
        this.bytesCallable = null;
    }

    public String getRelativePath() {
        return relativePath;
    }


    @Deprecated
    public List<FileData> getAttachment() {
        return Collections.unmodifiableList(attachment);
    }

    public void traverseAttachmentOnly(Consumer<FileData> consumer) {
        for (FileData fileData : Collections.unmodifiableList(attachment)) {
            consumer.accept(fileData);
        }
    }

    public void traverseAll(Consumer<FileData> consumer) {
        consumer.accept(this);
        traverseAttachmentOnly(fileData -> fileData.traverseAll(consumer));
    }

    public List<FileData> allFiles() {
        List<FileData> result = new ArrayList<>();
        traverseAll(result::add);
        return result;
    }

    public void attach(FileData fileData) {
        synchronized (this) {
            if (attachment.isEmpty()) {
                attachment = new ArrayList<>();
            }
            attachment.add(fileData);
        }
    }

    public boolean isDeleted() {
        return getBytes() == null;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean contentLoaded() {
        return bytesCallable == null;
    }

    public interface LoadFunction {
        byte[] load(FileData fileData) throws IOException;
    }
}
