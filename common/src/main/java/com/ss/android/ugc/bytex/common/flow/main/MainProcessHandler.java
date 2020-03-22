package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.transformer.TransformEngine;
import com.ss.android.ugc.bytex.transformer.cache.FileData;
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MainProcessHandler {


    /**
     * 请优先使用{@link #init(TransformEngine)}
     * Use {@link #init(TransformEngine)} instead.
     */
    @Deprecated
    void init();

    /**
     * 用于transform开始前初始化实现
     * used for initialization before transform started.
     *
     * @param transformer engine
     */
    default void init(@Nonnull TransformEngine transformer) {
        init();
    }

    /**
     * 遍历工程中所有的增量文件，不仅仅是class，如果是jar则会解压之后将entry传递进来
     * 状态可能为ADD,REMOVE,CHANGED这几种状态,只在增量构建时有效
     * <p>
     * traverse all incremental file which status is ADD,REMOVE or CHANGED
     * file will be uncompressed which is jar input.
     * only valid while during incremental build
     *
     * @param fileData 增量文件
     *                 incremental file
     * @param chain    如果是class，则会传递对应的ClassVisitorChain用于加入自定义的ClassVisitor，如果有不是class 则为null
     *                 If it is a class, the corresponding ClassVisitorChain will be passed to add a custom ClassVisitor, or null if there is not a class
     */
    default void traverseIncremental(@Nonnull FileData fileData, @Nullable ClassVisitorChain chain) {
    }

    /**
     * 遍历工程中所有的增量文件，不仅仅是class，如果是jar则会解压之后将entry传递进来
     * 状态可能为ADD,REMOVE,CHANGED这几种状态,只在增量构建时有效
     * <p>
     * traverse all incremental file which status is ADD,REMOVE or CHANGED
     * file will be uncompressed which is jar input.
     * only valid while during incremental build
     *
     * @param fileData 增量文件，该文件一定是class文件。
     *                 Incremental file, and the file must be a class file.
     * @param node     对应的Class解析后的的Tree Node结构
     *                 Tree Node
     */
    default void traverseIncremental(@Nonnull FileData fileData, @Nonnull ClassNode node) {
    }

    /**
     * 用于transform前的准备工作,在traverseIncremental之后，traverse方法之前执行。可以判断是否走增量
     * It is used for preparation before transform, and is executed after traverseIncremental
     * and before traverse method. Can determine whether to take incremental
     *
     * @param transformer from TransformEngine
     */
    default void beforeTraverse(@Nonnull TransformEngine transformer) {
    }

    /**
     * 遍历工程中所有的class
     * Traverse all classes in the whole project.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param chain        ClassVisitorChain用于加入自定义的ClassVisitor。 ASM visitor api. ClassVisitorChain is used for appending your custom ClassVisitor.
     */
    void traverse(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain);

    /**
     * 遍历工程中所有的class
     * Traverse all classes in the whole project.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param node         class的数据结构（照顾喜欢用tree api的同学）。 ASM tree api.
     */
    void traverse(@Nonnull String relativePath, @Nonnull ClassNode node);

    /**
     * 遍历android.jar中所有的class
     * Traverse all classes in android.jar.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param chain        ClassVisitorChain用于加入自定义的ClassVisitor。 ASM visitor api. ClassVisitorChain is used for appending your custom ClassVisitor.
     */
    void traverseAndroidJar(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain);

    /**
     * 遍历android.jar中所有的class
     * Traverse all classes in android.jar.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param node         class的数据结构（照顾喜欢用tree api的同学）。 ASM tree api.
     */
    void traverseAndroidJar(@Nonnull String relativePath, @Nonnull ClassNode node);

    /**
     * 用于transform前的准备工作
     * Prepare for real transform.
     *
     * @param transformer from TransformEngine
     */
    void beforeTransform(@Nonnull TransformEngine transformer);

    /**
     * handle 工程中的所有class
     * To process all classed in the whole project.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param chain        ClassVisitorChain用于注册自定义的ClassVisitor。 ASM visitor api. ClassVisitorChain is used for appending your custom ClassVisitor.
     * @return if false, this class file would be deleted, otherwise it would be output after transform.
     */
    boolean transform(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain);

    /**
     * handle 工程中的所有class
     * To process all classed in the whole project.
     *
     * @param relativePath class的相对路径。 relative path of class
     * @param node         class的数据结构（照顾喜欢用tree api的同学）。。 ASM tree api.
     * @return if false, this class file would be deleted, otherwise it would be output after transform.
     */
    boolean transform(@Nonnull String relativePath, @Nonnull ClassNode node);

    /**
     * 用于transform完成后，进行各种illegal check
     * It's usually used for plugin's custom illegal check after transform.
     *
     * @param transformer from TransformEngine
     */
    void afterTransform(@Nonnull TransformEngine transformer);

    /**
     * 在traverse和transform的过程中，加入自定义的FileProcessor，提供更大的灵活性。
     * Add custom FileProcessor during traverse or transform.
     *
     * @param process enum
     * @return list of FileProcessor
     */
    default List<FileProcessor> process(Process process) {
        return Collections.emptyList();
    }

    /**
     * Custom your own flag for ClassReader.
     * By default, `ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES` for traverse， `0` for transform.
     *
     * @param process enum
     * @return flag for ClassReader.
     */
    default int flagForClassReader(Process process) {
        switch (process) {
            case TRAVERSE:
            case TRAVERSE_ANDROID:
                return ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
            case TRANSFORM:
            default:
                return 0;
        }
    }

    /**
     * Custom your own flag for ClassWriter.
     * By default, `ClassWriter.COMPUTE_MAXS`
     * 默认应该是 ClassWriter.COMPUTE_MAXS，非特殊情况没必要复写
     *
     * @return flag for ClassWriter.
     */
    default int flagForClassWriter() {
        return ClassWriter.COMPUTE_MAXS;
    }

    /**
     * 这个Handler是不是要求在真正执行前进行一次校验
     * Verify bytecode  before transform.
     *
     * @return if true, indicate that need to verify bytecode. Default is false.
     */
    default boolean needPreVerify() {
        return false;
    }

    /**
     * 在执行完这个Handler后是否需要进行一次校验
     * Verify bytecode after transform.
     *
     * @return if true, indicate that need to verify bytecode. Default is false.
     */
    default boolean needVerify() {
        return false;
    }

    /**
     * Whether skip traverse, do transform only.
     *
     * @return if true, skip traverse(Note:ClassGraph will be invalid). Default is false.
     */
    default boolean isOnePassEnough() {
        return false;
    }
}