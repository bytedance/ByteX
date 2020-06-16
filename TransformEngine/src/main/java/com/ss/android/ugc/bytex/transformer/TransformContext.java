package com.ss.android.ugc.bytex.transformer;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.BaseVariant;
import com.ss.android.ugc.bytex.gradletoolkit.Artifact;
import com.ss.android.ugc.bytex.gradletoolkit.GradleEnv;
import com.ss.android.ugc.bytex.gradletoolkit.TransformEnv;
import com.ss.android.ugc.bytex.gradletoolkit.TransformInvocationKt;
import com.ss.android.ugc.bytex.transformer.cache.DirCache;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.cache.FileData;
import com.ss.android.ugc.bytex.transformer.cache.JarCache;
import com.ss.android.ugc.bytex.transformer.cache.NewFileCache;
import com.ss.android.ugc.bytex.transformer.io.ClassFinder;
import com.ss.android.ugc.bytex.transformer.io.ClassNodeLoader;
import com.ss.android.ugc.bytex.transformer.location.Locator;
import com.ss.android.ugc.bytex.transformer.utils.Service;

import org.gradle.api.Project;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class TransformContext implements GradleEnv, ClassFinder {
    private TransformInvocation invocation;
    private TransformEnv transformEnv;
    private Locator locator;
    private TransformInputs transformInputs;
    private TransformOutputs transformOutputs;
    protected Project project;
    protected AppExtension android;
    private boolean isPluginIncremental;
    private boolean shouldSaveCache;
    private File graphCacheFile;
    private String temporaryDirName;
    private ClassFinder finder;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TransformContext(TransformInvocation invocation, Project project, AppExtension android, boolean isPluginIncremental) {
        this(invocation, project, android, isPluginIncremental, true);
    }

    public TransformContext(TransformInvocation invocation, Project project, AppExtension android, boolean isPluginIncremental, boolean shouldSaveCache) {
        this(invocation, project, android, isPluginIncremental, shouldSaveCache, false);
    }

    public TransformContext(TransformInvocation invocation, Project project, AppExtension android, boolean isPluginIncremental, boolean shouldSaveCache, boolean useRawCache) {
        this.invocation = invocation;
        this.project = project;
        this.android = android;
        this.isPluginIncremental = isPluginIncremental;
        this.transformEnv = Service.load(TransformEnv.class);
        if (transformEnv != null) {
            transformEnv.setTransformInvocation(invocation);
        }
        temporaryDirName = invocation.getContext().getTemporaryDir().getName();
        temporaryDirName = temporaryDirName.replace("transformClassesAndResourcesWith", "");
        temporaryDirName = temporaryDirName.replace("transformClassesWith", "");
        graphCacheFile = new File(byteXBuildDir(), "graphCache.json");
        this.shouldSaveCache = shouldSaveCache;
        this.locator = new Locator(this);
        this.transformOutputs = new TransformOutputs(this, invocation, new File(byteXBuildDir(), "outputs.txt"), isPluginIncremental, shouldSaveCache, useRawCache);
        this.transformInputs = new TransformInputs(this, invocation, new File(byteXBuildDir(), "inputs.txt"), isPluginIncremental, shouldSaveCache, useRawCache);
        this.finder = new ClassNodeLoader(this);
    }

    public TransformInputs getTransformInputs() {
        return transformInputs;
    }

    public Collection<DirCache> getAllDirs() {
        return transformInputs.getAllDirs();
    }

    public Collection<JarCache> getAllJars() {
        return transformInputs.getAllJars();
    }

    public Stream<FileCache> allFiles() {
        return transformInputs.allFiles();
    }

    public TransformOutputs getTransformOutputs() {
        return transformOutputs;
    }

    public File getOutputFile(QualifiedContent content) throws IOException {
        return transformOutputs.getOutputFile(content);
    }

    public File getOutputFile(QualifiedContent content, boolean createIfNeed) throws IOException {
        return transformOutputs.getOutputFile(content, createIfNeed);
    }

    public static File getOutputTarget(File root, String relativePath) throws IOException {
        return TransformOutputs.getOutputTarget(root, relativePath);
    }

    public File getOutputDir(String affinity) throws IOException {
        return transformOutputs.getOutputDir(affinity);
    }

    public boolean isIncremental() {
        return invocation.isIncremental() && isPluginIncremental;
    }

    public boolean shouldSaveCache() {
        return shouldSaveCache;
    }

    /**
     * 请求非增量运行，必须在traverse时机之前调用<br/>
     * beforeTraverse及之前生命周期可调用，否则报RuntimeException<br/>
     */
    public void requestNotIncremental() {
        if (running.get()) {
            throw new RuntimeException("You Should request for not incremental before traversing.");
        }
        this.isPluginIncremental = false;
        this.transformInputs.requestNotIncremental();
    }

    /**
     * 请求某个文件进行非增量<br/>
     * beforeTraverse及之前生命周期可调用，否则报RuntimeException<br/>
     *
     * @param relativePath 文件的相对路径,比如 com/bytedance/Demo.class
     * @return 成功修改对应输入的状态，如果当前已经是非增量
     */
    public boolean requestNotIncremental(String relativePath) {
        if (running.get()) {
            throw new RuntimeException("You Should request for not incremental before traversing.");
        }
        if (!this.isIncremental()) {
            return false;
        }
        return this.transformInputs.requestNotIncremental(relativePath);
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
        transformInputs.addFile(affinity, file);
    }

    public List<FileData> getChangedFiles() {
        if (isIncremental()) {
            return transformInputs.getChangedFiles();
        } else {
            return Collections.emptyList();
        }
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
        transformInputs.saveCache();
        transformInputs.release();
        transformInputs = null;
        transformOutputs.saveCache();
        transformOutputs.release();
        transformOutputs = null;
        invocation = null;
        transformEnv = null;
        locator = null;
        project = null;
        android = null;
        graphCacheFile = null;
        temporaryDirName = null;
        finder = null;
    }

    @Override
    public ClassNode find(String className) {
        if (finder != null) {
            return finder.find(className);
        }
        return null;
    }

    @Override
    public Class<?> loadClass(String className) {
        if (finder != null) {
            return finder.loadClass(className);
        }
        return null;
    }
}

