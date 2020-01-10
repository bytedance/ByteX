package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.Status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by tlh on 2018/8/22.
 */

public class FileData implements Serializable {
    private byte[] bytes;
    private String relativePath;
    private List<FileData> attachment = Collections.emptyList();
    private Status status = Status.NOTCHANGED;

    public FileData(byte[] bytes, String relativePath) {
        this(bytes, relativePath, Status.NOTCHANGED);
    }

    public FileData(byte[] bytes, String relativePath, Status status) {
        this.bytes = bytes;
        this.relativePath = relativePath;
        this.status = status;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void delete() {
        this.bytes = null;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public List<FileData> getAttachment() {
        return attachment;
    }

    public void attach(FileData fileData) {
        if (attachment.isEmpty()) {
            attachment = new ArrayList<>();
        }
        attachment.add(fileData);
    }

    public boolean isDeleted() {
        return bytes == null;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
