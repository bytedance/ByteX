package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.utils.FileUtils;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class JarCache extends FileCache {
    private final File jar;
    private final Status status;
    private final File outputFile;

    public JarCache(QualifiedContent content, TransformContext context) {
        super(content, context);
        jar = content.getFile();
        status = ((JarInput) content).getStatus();
        try {
            outputFile = context.getOutputFile(content, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JarCache(File jar, TransformContext context) {
        super(null, context);
        this.jar = jar;
        status = Status.ADDED;
        outputFile = null;
    }

    @Override
    public void transformOutput(Consumer<FileData> visitor) throws IOException {
        if (context.isIncremental()) {
            switch (status) {
                case NOTCHANGED:
                    return;
                case REMOVED:
                    throw new IllegalStateException("REMOVED File Can Not Output:" + outputFile.getAbsolutePath());
                case CHANGED:
                    FileUtils.deleteIfExists(outputFile);
                    break;
                case ADDED:
                    break;
            }
        }
        if (!outputFile.exists()) {
            Files.createParentDirs(outputFile);
        }
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        try {
            forEach(file -> {
                try {
                    if (visitor != null) visitor.accept(file);
                    String relativePath = file.getRelativePath();
                    byte[] raw = file.getBytes();
                    if (raw != null && raw.length > 0) {
                        ZipEntry entry = new ZipEntry(relativePath);
                        jos.putNextEntry(entry);
                        jos.write(raw);
                    }
                    for (FileData attachment : file.getAttachment()) {
                        raw = attachment.getBytes();
                        relativePath = attachment.getRelativePath();
                        if (raw != null && raw.length > 0) {
                            ZipEntry entry = new ZipEntry(relativePath);
                            jos.putNextEntry(entry);
                            jos.write(raw);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            jos.close();
        }
    }

    @Override
    protected List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException {
        if (context.isIncremental()) {
            // Skip NOTCHANGED
            if (status == Status.NOTCHANGED) {
                return Collections.emptyList();
            } else if (status == Status.REMOVED) {
                FileUtils.deleteIfExists(outputFile);
                return Collections.emptyList();
            }
        }
        if (!jar.exists()) {
            return Collections.emptyList();
        }
        List<FileData> dataList = new ArrayList<>();
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(jar)));
        ZipEntry zipEntry;
        try {
            while ((zipEntry = zin.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                byte[] raw = ByteStreams.toByteArray(zin);
                FileData data = new FileData(raw, zipEntry.getName(), context.isIncremental() ? status : Status.ADDED);
                if (emitter != null) {
                    emitter.onNext(data);
                }
                dataList.add(data);
            }
        } finally {
            zin.close();
        }
        return dataList;
    }

    @Override
    public void skip() throws IOException {
        File dest = context.getOutputFile(content);
        FileUtils.copyFile(jar, dest);
    }

    @Override
    public List<FileData> getChangedFiles() {
        try {
            return resolve(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getFile() {
        return jar;
    }
}
