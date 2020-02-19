package com.ss.android.ugc.bytex.transformer;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.ss.android.ugc.bytex.gradletoolkit.Artifact;
import com.ss.android.ugc.bytex.gradletoolkit.GradleEnv;
import com.ss.android.ugc.bytex.gradletoolkit.TransformEnv;
import com.ss.android.ugc.bytex.gradletoolkit.TransformInvocationKt;
import com.ss.android.ugc.bytex.transformer.cache.DirCache;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.cache.FileData;
import com.ss.android.ugc.bytex.transformer.cache.JarCache;
import com.ss.android.ugc.bytex.transformer.cache.NewFileCache;
import com.ss.android.ugc.bytex.transformer.location.Locator;
import com.ss.android.ugc.bytex.transformer.utils.Service;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class TransformContext implements GradleEnv {
    private TransformInvocation invocation;
    private TransformEnv transformEnv;
    private Locator locator;
    private Collection<JarCache> allJars;
    private Collection<DirCache> allDirs;
    private Map<String, NewFileCache> newDirs;
    protected Project project;
    protected AppExtension android;
    private boolean isIncremental;
    private boolean shouldSaveCache;
    private File graphCacheFile;
    private String temporaryDirName;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TransformContext(TransformInvocation invocation, Project project, AppExtension android, boolean isIncremental, boolean shouldSaveCache) {
        this.invocation = invocation;
        this.project = project;
        this.android = android;
        this.isIncremental = isIncremental;
        this.transformEnv = Service.load(TransformEnv.class);
        if (transformEnv != null) {
            transformEnv.setTransformInvocation(invocation);
        }
        temporaryDirName = invocation.getContext().getTemporaryDir().getName();
        temporaryDirName = temporaryDirName.replace("transformClassesAndResourcesWith", "");
        temporaryDirName = temporaryDirName.replace("transformClassesWith", "");
        if (isIncremental) {
            graphCacheFile = new File(byteXBuildDir(), "graphCache.json");
        }
        this.shouldSaveCache = shouldSaveCache;
        this.locator = new Locator(this);
        init();
    }

    private void init() {
        allJars = new ArrayList<>(invocation.getInputs().size());
        allDirs = new ArrayList<>(invocation.getInputs().size());
        newDirs = new HashMap<>();
        invocation.getInputs().forEach(input -> {
            allJars.addAll(input.getJarInputs().stream().map(i -> new JarCache(i, this)).collect(Collectors.toList()));
            allDirs.addAll(input.getDirectoryInputs().stream().map(i -> new DirCache(i, this)).collect(Collectors.toList()));
        });
    }

    public Collection<DirCache> getAllDirs() {
        return Collections.unmodifiableCollection(allDirs);
    }

    public Collection<JarCache> getAllJars() {
        return Collections.unmodifiableCollection(allJars);
    }

    public Stream<FileCache> allFiles() {
        return Streams.concat(allDirs.stream(), allJars.stream(), newDirs.values().stream());
    }

    public File getOutputFile(QualifiedContent content) throws IOException {
        return getOutputFile(content, true);
    }

    public File getOutputFile(QualifiedContent content, boolean createIfNeed) throws IOException {
        File target = invocation.getOutputProvider().getContentLocation(content.getName(), content.getContentTypes(), content.getScopes(),
                content instanceof JarInput ? Format.JAR : Format.DIRECTORY);
        if (createIfNeed && !target.exists()) {
            Files.createParentDirs(target);
        }
        return target;
    }

    public static File getOutputTarget(File root, String relativePath) throws IOException {
        File target = new File(root, relativePath.replace('/', File.separatorChar));
        if (!target.exists()) {
            Files.createParentDirs(target);
        }
        return target;
    }

    public File getOutputDir(String affinity) throws IOException {
        File root = invocation.getOutputProvider().getContentLocation(affinity, Collections.singleton(QualifiedContent.DefaultContentType.CLASSES),
                TransformManager.SCOPE_FULL_PROJECT, Format.DIRECTORY);
        if (!root.exists()) {
            Files.createParentDirs(root);
        }
        return root;
    }

    public boolean isIncremental() {
        return invocation.isIncremental() && isIncremental;
    }

    public boolean shouldSaveCache() {
        return shouldSaveCache;
    }

    /**
     * 请求非增量运行，必须在traverse时机之前调用
     */
    public void requestNotIncremental() {
        if (running.get()) {
            throw new RuntimeException("You Should request for not incremental before traversing.");
        }
        this.isIncremental = false;
    }

    public boolean isReleaseBuild() {
        return invocation.getContext().getVariantName().toLowerCase().contains("release");
    }

    public TransformInvocation getInvocation() {
        return invocation;
    }

    public String getVariantName() {
        return invocation.getContext().getVariantName();
    }

    public BaseVariant getVariant() {
        return TransformInvocationKt.getVariant(invocation);
    }

    public File androidJar() throws FileNotFoundException {
        File jar = new File(getSdkJarDir(), "android.jar");
        if (!jar.exists()) {
            throw new FileNotFoundException("Android jar not found!");
        }
        return jar;
    }

    private String getSdkJarDir() {
        String compileSdkVersion = android.getCompileSdkVersion();
        return String.join(File.separator, android.getSdkDirectory().getAbsolutePath(), "platforms", compileSdkVersion);
    }

    public File byteXBuildDir() {
        return new File(new File(project.getBuildDir(), "ByteX"), temporaryDirName);
    }

    @Nonnull
    @Override
    public Collection<File> getArtifact(@Nonnull Artifact artifact) {
        return transformEnv.getArtifact(artifact);
    }

    void addFile(String affinity, FileData file) {
        newDirs.computeIfAbsent(affinity, k -> new NewFileCache(this, affinity)).addFile(file);
    }

    public List<FileData> getChangedFiles() {
        return allFiles().flatMap(fileCache -> fileCache.getChangedFiles().stream()).collect(Collectors.toList());
    }

    public Locator getLocator() {
        return locator;
    }

    /**
     * 返回Transform的Graph缓存文件路径，增量编译需要读取和写入使用
     *
     * @return 缓存文件, 如果不支持增量，返回false
     */
    public File getGraphCache() {
        return graphCacheFile;
    }


    void markRunningState(boolean running) {
        this.running.set(running);
    }

    public void release() {
        invocation = null;
        transformEnv = null;
        locator = null;
        allJars = null;
        allDirs = null;
        newDirs = null;
        project = null;
        android = null;
        graphCacheFile = null;
        temporaryDirName = null;
    }
}

