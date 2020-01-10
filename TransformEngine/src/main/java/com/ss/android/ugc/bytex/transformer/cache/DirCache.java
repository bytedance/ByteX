package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.utils.FileUtils;
import com.google.common.io.Files;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.io.Files_;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class DirCache extends FileCache {
    private final File dir;
    private final File outputFile;

    public DirCache(QualifiedContent content, TransformContext context) {
        super(content, context);
        dir = content.getFile();
        try {
            outputFile = context.getOutputFile(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public void transformOutput(Consumer<FileData> visitor) throws IOException {
        forEach(fileData -> {
            try {
                if (visitor != null) visitor.accept(fileData);
                byte[] bytes = fileData.getBytes();
                if (bytes != null && bytes.length > 0) {
                    File target = TransformContext.getOutputTarget(outputFile, fileData.getRelativePath());
                    if (context.isIncremental()) {
                        switch (fileData.getStatus()) {
                            case NOTCHANGED:
                                return;
                            case REMOVED:
                                throw new IllegalStateException("REMOVED File Can Not Output:" + fileData.getRelativePath());
                            case CHANGED:
                                FileUtils.deleteIfExists(target);
                                break;
                            case ADDED:
                                break;
                        }
                    }
                    if (!target.exists()) {
                        Files.createParentDirs(target);
                    }
                    Files.write(bytes, target);
                }
                for (FileData attachment : fileData.getAttachment()) {
                    bytes = attachment.getBytes();
                    if (bytes != null && bytes.length > 0) {
                        File target = TransformContext.getOutputTarget(outputFile, attachment.getRelativePath());
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
        URI base = dir.toURI();
        List<FileData> dataList = new ArrayList<>();
        if (context.isIncremental()) {
            Set<String> changeFileNames = new HashSet<>();
            for (Map.Entry<File, Status> entry : ((DirectoryInput) content).getChangedFiles().entrySet()) {
                File file = entry.getKey();
                if (entry.getValue() == Status.REMOVED) {
                    //删除文件
                    FileUtils.deleteIfExists(TransformContext.getOutputTarget(outputFile, base.relativize(file.toURI()).toString()));
                } else if (file.isFile() && !file.getName().equalsIgnoreCase(".DS_Store")) {
                    String relativePath = base.relativize(file.toURI()).toString();
                    byte[] raw = Files.toByteArray(file);
                    FileData data = new FileData(raw, relativePath, entry.getValue());
                    emitter.onNext(data);
                    changeFileNames.add(relativePath);
                    dataList.add(data);
                }
            }
        } else {
            for (File file : Files_.fileTreeTraverser().preOrderTraversal(dir)) {
                if (file.isFile() && !file.getName().equalsIgnoreCase(".DS_Store")) {
                    String relativePath = base.relativize(file.toURI()).toString();
                    byte[] raw = Files.toByteArray(file);
                    FileData data = new FileData(raw, relativePath, Status.ADDED);
                    emitter.onNext(data);
                    dataList.add(data);
                }
            }
        }
        return dataList;
    }

    @Override
    public void skip() throws IOException {
        File dest = context.getOutputFile(content);
        FileUtils.copyDirectory(dir, dest);
    }

    @Override
    public List<FileData> getChangedFiles() {
        if (context.isIncremental()) {
            List<FileData> changedFiles = new LinkedList<>();
            URI base = dir.toURI();
            try {
                for (Map.Entry<File, Status> entry : ((DirectoryInput) content).getChangedFiles().entrySet()) {
                    File file = entry.getKey();
                    if (file.isFile() && !file.getName().equalsIgnoreCase(".DS_Store")) {
                        String relativePath = base.relativize(file.toURI()).toString();
                        byte[] raw = Files.toByteArray(file);
                        FileData data = new FileData(raw, relativePath, entry.getValue());
                        changedFiles.add(data);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return changedFiles;
        } else return Collections.emptyList();
    }
}
