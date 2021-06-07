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
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class JarCache extends FileCache {
    private final File jar;
    private final Status status;
    private final File outputFile;
    private boolean useFixedTimestamp = false;


    public JarCache(QualifiedContent content, TransformContext context) {
        this(content, context, false);
    }

    public JarCache(QualifiedContent content, TransformContext context, boolean useFixedTimestamp) {
        super(content, context);
        jar = content.getFile();
        status = ((JarInput) content).getStatus();
        this.useFixedTimestamp = useFixedTimestamp;
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
        output();
        List<FileData> dataList = Collections.synchronizedList(new LinkedList<>());
        forEach(item -> {
            if (visitor != null) visitor.accept(item);
            dataList.add(item);
        });
        String outputRelativePath = context.getTransformOutputs().relativeToProject(outputFile);
        TransformOutputs.Entry outputs = context.getTransformOutputs().getLastTransformOutputs().get(outputRelativePath);
        boolean isHookJar = outputFile.getAbsolutePath().equals(getFile().getAbsolutePath());
        boolean needOutput = !context.getInvocation().isIncremental() || isHookJar;
        if (!needOutput) {
            needOutput = dataList.stream()
                    .flatMap((Function<FileData, Stream<FileData>>) fileData -> fileData.allFiles().stream())
                    .anyMatch(fileData -> fileData.getStatus() != Status.NOTCHANGED);
        }
        if (needOutput || dataList.isEmpty()) {
            if (dataList.isEmpty()) {
                FileUtils.deleteIfExists(outputFile);
            }
            dataList.sort((t0, t1) -> t0.getRelativePath().compareTo(t1.getRelativePath()));
            TransformOutputs.Entry mapToEntry = new TransformOutputs.Entry(
                    context.getTransformOutputs().relativeToProject(getFile()),
                    outputRelativePath,
                    outputFile.exists() ? TransformOutputs.Entry.Companion.hash(outputFile) : TransformOutputs.Entry.INVALID_HASH,
                    dataList.stream().map(fileData -> TransformOutputs.Entry.Companion.outputEntry(fileData, outputRelativePath)).sorted().collect(Collectors.toList()));
            if (dataList.size() > 0 && (!outputFile.exists() || outputs == null || outputs.getIdentify() != mapToEntry.getIdentify())) {
                //输出的结果不一样
                AtomicBoolean hasOutput = new AtomicBoolean(false);
                File copyFile = Files.createTempFile(String.valueOf(outputFile.getAbsolutePath().hashCode()), "").toFile();
                JarFile jarFile = null;
                try {
                    if (!isHookJar && outputFile.exists() && dataList.stream()
                            .flatMap((Function<FileData, Stream<FileData>>) fileData -> fileData.allFiles().stream())
                            .anyMatch(fileData -> fileData.getStatus() == Status.NOTCHANGED)) {
                        //增量构建，如果存在fileData是NOTCHANGED,则需要从原来的outputFile去读取entry
                        FileUtils.copyFile(outputFile, copyFile);
                        jarFile = new JarFile(copyFile);
                    }
                    String reason = "Unknown";
                    if (isHookJar) {
                        reason = "Hook Jar";
                    } else if (!outputFile.exists()) {
                        reason = "outputFile not exists(Most likely it is full compilation)";
                    } else if (outputs == null) {
                        reason = "lost last outputs";
                    } else if (outputs.getIdentify() != mapToEntry.getIdentify()) {
                        reason = "content changed:" + outputs.getIdentify() + "->" + mapToEntry.getIdentify();
                    }
                    System.out.println("ByteX outputJar:" + getFile().getAbsolutePath() + "->" + outputFile.getAbsolutePath() + ":reason:" + reason);
                    try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                        for (FileData item : dataList) {
                            for (FileData file : item.allFiles()) {
                                byte[] bytes;
                                switch (file.getStatus()) {
                                    case ADDED:
                                    case CHANGED:
                                        bytes = file.getBytes();
                                        break;
                                    case NOTCHANGED:
                                        if (jarFile == null) {
                                            throw new IllegalStateException("outputFile not exists:" + outputFile.getAbsolutePath());
                                        }
                                        bytes = ByteStreams.toByteArray(jarFile.getInputStream(jarFile.getEntry(file.getRelativePath())));
                                        break;
                                    case REMOVED:
                                        bytes = null;
                                        break;
                                    default:
                                        throw new IllegalStateException(file.getStatus().toString());
                                }
                                if (bytes != null) {
                                    ZipEntry entry = new ZipEntry(file.getRelativePath());
                                    if (useFixedTimestamp) {
                                        entry.setTime(0);
                                        entry.setCreationTime(FileTime.fromMillis(0L));
                                        entry.setLastAccessTime(FileTime.fromMillis(0L));
                                        entry.setLastModifiedTime(FileTime.fromMillis(0L));
                                    }
                                    jos.putNextEntry(entry);
                                    jos.write(bytes);
                                    hasOutput.set(true);
                                }
                            }
                        }
                    }
                } finally {
                    if (jarFile != null) {
                        jarFile.close();
                    }
                    FileUtils.delete(copyFile);
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
                        System.out.println("ByteX LoadJar:" + getFile().getAbsolutePath());
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
            if (status == Status.NOTCHANGED && items != null) {
                for (String item : items) {
                    dataMap.put(item, new FileData(loadFunction, item, Status.NOTCHANGED));
                }
            } else {
                //读所有的entry
                try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(getFile())))) {
                    ZipEntry zipEntry;
                    while ((zipEntry = zin.getNextEntry()) != null) {
                        if (!zipEntry.isDirectory()) {
                            if (status == Status.NOTCHANGED) {
                                //NOTCHANGED，延迟加载,绝大部分情况不会加载，降低内存
                                dataMap.put(zipEntry.getName(), new FileData(loadFunction, zipEntry.getName(), Status.NOTCHANGED));
                            } else {
                                //ADD或者CHANGED
                                Status thisStatus = status;
                                if (status == Status.CHANGED && items != null && !items.contains(zipEntry.getName())) {
                                    //修改了，并且上次输入中没有，算是新增
                                    thisStatus = Status.ADDED;
                                }
                                dataMap.put(zipEntry.getName(), new FileData(ByteStreams.toByteArray(zin), zipEntry.getName(), thisStatus));
                            }
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
            }
            if (emitter != null) {
                dataMap.values().forEach(emitter::onNext);
            }
            return new ArrayList<>(dataMap.values());
        } else {
            File file = getFile();
            if(!file.exists()){
                return Collections.emptyList();
            }
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
        output();
        FileUtils.copyFile(getFile(), outputFile);
    }

    @Override
    public File getFile() {
        return jar;
    }
}
