package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.utils.FileUtils;
import com.google.common.io.ByteStreams;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformOutputs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
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
        this(jar, Status.ADDED, context);
    }

    public JarCache(File jar, Status status, TransformContext context) {
        super(null, context);
        this.jar = jar;
        this.status = status;
        outputFile = null;
    }

    @Override
    public final synchronized void transformOutput(Consumer<FileData> visitor) throws IOException {
        if (hasWritten) {
            throw new RuntimeException("rewrite");
        }
        List<FileData> dataList = Collections.synchronizedList(new LinkedList<>());
        AtomicBoolean needOutput = new AtomicBoolean(!context.getInvocation().isIncremental());
        forEach(item -> {
            if (visitor != null) visitor.accept(item);
            dataList.add(item);
            item.traverseAll(fileData -> {
                if (fileData.getStatus() != Status.NOTCHANGED) {
                    needOutput.set(true);
                }
            });
        });
        String outputRelativePath = context.getTransformOutputs().relativeToProject(outputFile);
        String inputRelativePath = context.getTransformOutputs().relativeToProject(getFile());
        TransformOutputs.Entry outputs = context.getTransformOutputs().getLastTransformOutputs().get(outputRelativePath);
        if (needOutput.get() || dataList.isEmpty()) {
            if (dataList.isEmpty()) {
                FileUtils.deleteIfExists(outputFile);
            }
            TransformOutputs.Entry mapToEntry = new TransformOutputs.Entry(
                    inputRelativePath,
                    outputRelativePath,
                    outputFile.exists() ? TransformOutputs.Entry.Companion.hash(outputFile) : TransformOutputs.Entry.INVALID_HASH,
                    dataList.stream().map(fileData -> TransformOutputs.Entry.Companion.outputEntry(fileData, outputRelativePath)).sorted().collect(Collectors.toList()));
            if (dataList.size() > 0 && (!outputFile.exists() || outputs == null || outputs.getIdentify() != mapToEntry.getIdentify())) {
                //输出的结果不一样
                AtomicBoolean hasOutput = new AtomicBoolean(false);
                try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                    for (FileData item : dataList) {
                        item.traverseAll(fileData -> {
                            try {
                                if (fileData.getStatus() != Status.REMOVED) {
                                    byte[] bytes = fileData.getBytes();
                                    if (bytes != null && bytes.length > 0) {
                                        ZipEntry entry = new ZipEntry(fileData.getRelativePath());
                                        jos.putNextEntry(entry);
                                        jos.write(bytes);
                                        hasOutput.set(true);
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                if (!hasOutput.get()) {
                    FileUtils.deleteIfExists(outputFile);
                }
                mapToEntry = new TransformOutputs.Entry(
                        mapToEntry.getInput(),
                        mapToEntry.getPath(),
                        outputFile.exists() ? TransformOutputs.Entry.Companion.hash(outputFile) : TransformOutputs.Entry.INVALID_HASH,
                        mapToEntry.getExtras());
            }
            outputs = mapToEntry;

        }
        context.getTransformOutputs().getTransformOutputs().put(outputRelativePath, outputs);
        hasWritten = true;
    }

    @Override
    protected List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException {
        if (context.getInvocation().isIncremental()) {
            Set<String> items = context.getTransformInputs().getLastTransformInputs().get(getFile().getAbsolutePath());
            if (status == Status.REMOVED) {
                if (outputFile != null) {
                    FileUtils.deleteIfExists(outputFile);
                }
                if (items == null) {
                    return Collections.emptyList();
                } else {
                    return items.stream().map(s -> {
                        FileData fileData = new FileData((byte[]) null, s, Status.REMOVED);
                        if (emitter != null) {
                            emitter.onNext(fileData);
                        }
                        return fileData;
                    }).collect(Collectors.toList());
                }
            }
            final Map<String, FileData> dataMap = new ConcurrentHashMap<>();
            final AtomicBoolean once = new AtomicBoolean(false);
            FileData.LoadFunction loadFunction = fileData -> {
                synchronized (dataMap) {
                    if (once.compareAndSet(false, true)) {
                        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(getFile())))) {
                            ZipEntry zipEntry;
                            while ((zipEntry = zin.getNextEntry()) != null) {
                                if (!zipEntry.isDirectory()) {
                                    FileData item = dataMap.get(zipEntry.getName());
                                    if (!item.contentLoaded()) {
                                        item.setBytes(ByteStreams.toByteArray(zin), item.getStatus());
                                    }
                                }
                            }
                        }
                        for (FileData value : dataMap.values()) {
                            if (!value.contentLoaded()) {
                                throw new RuntimeException(value.getRelativePath() + "unloaded");
                            }
                        }
                    }
                }
                return fileData.getBytes();
            };
            //读所有的entry
            try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(getFile())))) {
                ZipEntry zipEntry;
                while ((zipEntry = zin.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory()) {
                        Status thisStatus = status;
                        if (status == Status.CHANGED && items != null && !items.contains(zipEntry.getName())) {
                            //修改了，并且上次输入中没有，算是新增
                            thisStatus = Status.ADDED;
                        }
                        dataMap.put(zipEntry.getName(), new FileData(loadFunction, zipEntry.getName(), thisStatus));
                    }
                }
            }
            if (status == Status.CHANGED && items != null) {
                //上次有但本次没有的算是删除
                for (String item : items) {
                    if (!dataMap.containsKey(item)) {
                        dataMap.put(item, new FileData((byte[]) null, item, Status.REMOVED));
                    }
                }
            }
            if (emitter != null) {
                dataMap.values().forEach(emitter::onNext);
            }
            return new ArrayList<>(dataMap.values());
        } else {
            List<FileData> dataList = new ArrayList<>();
            try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(getFile())))) {
                ZipEntry zipEntry;
                while ((zipEntry = zin.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory()) {
                        FileData data = new FileData(ByteStreams.toByteArray(zin), zipEntry.getName(), Status.ADDED);
                        if (emitter != null) {
                            emitter.onNext(data);
                        }
                        dataList.add(data);
                    }
                }
            }
            return dataList;
        }
    }

    @Override
    public synchronized void skip() throws IOException {
        if (hasWritten) {
            throw new RuntimeException("rewrite");
        }
        FileUtils.copyFile(getFile(), outputFile);
        hasWritten = true;
    }

    @Override
    public File getFile() {
        return jar;
    }
}
