package com.ss.android.ugc.bytex.transformer.cache;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.utils.FileUtils;
import com.google.common.io.Files;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.TransformOutputs;
import com.ss.android.ugc.bytex.transformer.io.Files_;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class DirCache extends FileCache {
    protected final File dir;
    protected final File outputFile;

    public DirCache(QualifiedContent content, TransformContext context) {
        super(content, context);
        dir = content.getFile();
        try {
            outputFile = context.getOutputFile(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DirCache(File dir, File outputFile, TransformContext context) {
        super(null, context);
        this.dir = dir;
        this.outputFile = outputFile;
    }

    @Override
    public File getFile() {
        return dir;
    }

    @Override
    public final synchronized void transformOutput(Consumer<FileData> visitor) throws IOException {
        if (hasWritten) {
            throw new RuntimeException("rewrite");
        }
        Map<String, TransformOutputs.Entry> entryMap = new HashMap<>();
        String relativeToProject = context.getTransformOutputs().relativeToProject(outputFile);
        TransformOutputs.Entry entry = context.getTransformOutputs().getLastTransformOutputs().get(relativeToProject);
        if (entry != null) {
            //建立索引
            entry.traverseAll(e -> entryMap.put(e.getPath(), e));
        }
        List<TransformOutputs.Entry> entries = Collections.synchronizedList(new LinkedList<>());
        parallelForEach(false, item -> {
            if (visitor != null) visitor.accept(item);
            entries.add(
                    transformOutput(
                            context.getTransformOutputs().relativeToProject(new File(getFile(), item.getRelativePath())),
                            relativeToProject,
                            item,
                            entryMap)
            );
        });
        Collections.sort(entries);
        context.getTransformOutputs().getTransformOutputs().put(relativeToProject,
                new TransformOutputs.Entry(context.getTransformOutputs().relativeToProject(getFile()),
                        relativeToProject,
                        0L,
                        Collections.unmodifiableList(entries))
        );
        hasWritten = true;
    }

    private TransformOutputs.Entry transformOutput(String input, String parent, FileData fileData, Map<String, TransformOutputs.Entry> entryMap) throws IOException {
        String relativePath = fileData.getRelativePath();
        String absolutePath = parent + "/" + fileData.getRelativePath();
        TransformOutputs.Entry entry = entryMap.get(relativePath);
        long hash = entry == null ? TransformOutputs.Entry.INVALID_HASH : entry.getHash();
        File target = TransformContext.getOutputTarget(outputFile, fileData.getRelativePath());
        if (fileData.getStatus() == Status.REMOVED) {
            //被移除了
            FileUtils.deleteIfExists(target);
        } else if (fileData.getStatus() != Status.NOTCHANGED) {
            if (fileData.contentLoaded()) {
                byte[] bytes = fileData.getBytes();
                if (bytes != null && bytes.length > 0) {
                    hash = TransformOutputs.Entry.Companion.hash(bytes);
                    if (!target.exists() || entry == null || hash != entry.getHash()) {
                        //hash有变化
                        if (!target.exists()) {
                            Files.createParentDirs(target);
                        }
                        Files.write(bytes, target);
                    } else {
                        //虽然是change状态，但和上次构建的输出hash是一样的，我们可以跳过赋值，这样就可以让后面的插件再次走增量
                    }
                } else {
                    //实际上是等价于被删除了
                    FileUtils.deleteIfExists(target);
                }
            } else {
                //考虑增量是请求了变更但没有加载，依然算是未改变
                //直接复制，减少io
                FileUtils.copyFile(new File(getFile(), fileData.getRelativePath()), target);
            }
        }
        if (!target.exists()) {
            hash = TransformOutputs.Entry.INVALID_HASH;
        } else if (hash == TransformOutputs.Entry.INVALID_HASH) {
            hash = TransformOutputs.Entry.Companion.hash(target);
        } else {
            //sikp check
//            if (hash != TransformOutputs.Entry.Companion.hash(target)) {
//                throw new IllegalStateException();
//            }
        }
        //输出附带新增的文件
        List<TransformOutputs.Entry> attachments = Collections.synchronizedList(new LinkedList<>());
        fileData.traverseAttachmentOnly(attachment -> {
            try {
                attachments.add(transformOutput(null, absolutePath, attachment, entryMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Collections.sort(attachments);
        return new TransformOutputs.Entry(input, relativePath, hash, Collections.unmodifiableList(attachments));
    }

    @Override
    protected List<FileData> resolve(ObservableEmitter<FileData> emitter) throws IOException {
        URI base = getFile().toURI();
        List<FileData> dataList = new ArrayList<>();
        if (context.getInvocation().isIncremental()) {
            FileData.LoadFunction loadFunction = fileData -> {
                try {
                    return Files.toByteArray(new File(getFile(), fileData.getRelativePath()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            //拿到所有的文件相对路径
            Set<String> allFiles = Files_.fileTreeTraverser()
                    .preOrderTraversal(getFile())
                    .stream()
                    .filter(File::isFile)
                    .filter(file -> !file.getName().equalsIgnoreCase(".DS_Store"))
                    .map(file -> base.relativize(file.toURI()).toString())
                    .collect(Collectors.toSet());
            //拿到所有的改变的文件
            for (Map.Entry<File, Status> entry : ((DirectoryInput) content).getChangedFiles().entrySet()) {
                File file = entry.getKey();
                if (!file.getName().equalsIgnoreCase(".DS_Store")) {
                    String relativePath = base.relativize(file.toURI()).toString();
                    FileData data = entry.getValue() == Status.REMOVED ?
                            new FileData((byte[]) null, relativePath, entry.getValue()) :
                            new FileData(loadFunction, relativePath, entry.getValue());
                    if (emitter != null) {
                        emitter.onNext(data);
                    }
                    dataList.add(data);
                    allFiles.remove(relativePath);
                }
            }
            //Not Change
            for (String file : allFiles) {
                FileData data = new FileData(loadFunction, file, Status.NOTCHANGED);
                if (emitter != null) {
                    emitter.onNext(data);
                }
                dataList.add(data);
            }
        } else {
            for (File file : Files_.fileTreeTraverser().preOrderTraversal(getFile())) {
                if (file.isFile() && !file.getName().equalsIgnoreCase(".DS_Store")) {
                    FileData data = new FileData(Files.toByteArray(file), base.relativize(file.toURI()).toString(), Status.ADDED);
                    if (emitter != null) {
                        emitter.onNext(data);
                    }
                    dataList.add(data);
                }
            }
        }
        return dataList;
    }

    @Override
    public synchronized void skip() throws IOException {
        if (hasWritten) {
            throw new RuntimeException("rewrite");
        }
        FileUtils.copyDirectory(getFile(), outputFile);
        hasWritten = true;
    }
}
