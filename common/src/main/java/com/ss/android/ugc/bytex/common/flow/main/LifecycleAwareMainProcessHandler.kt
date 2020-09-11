package com.ss.android.ugc.bytex.common.flow.main

import com.ss.android.ugc.bytex.common.utils.executeWithStartAndFinish
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain
import com.ss.android.ugc.bytex.transformer.TransformEngine
import com.ss.android.ugc.bytex.transformer.cache.FileData
import com.ss.android.ugc.bytex.transformer.processor.FileProcessor
import org.objectweb.asm.tree.ClassNode

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */

class LifecycleAwareMainProcessHandler(val real: MainProcessHandler) : MainProcessHandler by real {
    init {
        MainProcessHandler::class.java.methods.filter {
            it.isDefault
        }.forEach {
            try {
                LifecycleAwareMainProcessHandler::class.java.getDeclaredMethod(it.name, *it.parameterTypes)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("default methods in java interface must be overridden while using `by operation` in kotlin", e)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return real.equals(other)
    }

    override fun hashCode(): Int {
        return real.hashCode()
    }

    override fun toString(): String {
        return real.toString()
    }


    override fun init() {
        real.init()
    }

    override fun init(transformer: TransformEngine) {
        executeWithStartAndFinish(
                { real.init(transformer) },
                { GlobalMainProcessHandlerListener.startInit(real, transformer) },
                { e -> GlobalMainProcessHandlerListener.finishInit(real, transformer, e) }
        )

    }

    override fun traverseIncremental(fileData: FileData, chain: ClassVisitorChain?) {
        real.traverseIncremental(fileData, chain)
    }

    override fun traverseIncremental(fileData: FileData, node: ClassNode) {
        real.traverseIncremental(fileData, node)
    }

    override fun beforeTraverse(transformer: TransformEngine) {
        executeWithStartAndFinish(
                { real.beforeTraverse(transformer) },
                { GlobalMainProcessHandlerListener.startBeforeTraverse(real, transformer) },
                { e -> GlobalMainProcessHandlerListener.finishBeforeTraverse(real, transformer, e) }
        )
    }

    override fun startRunning(transformer: TransformEngine) {
        executeWithStartAndFinish(
                { real.startRunning(transformer) },
                { GlobalMainProcessHandlerListener.startStartRunning(real, transformer) },
                { e -> GlobalMainProcessHandlerListener.finishStartRunning(real, transformer, e) }
        )
    }

    override fun traverse(relativePath: String, chain: ClassVisitorChain) {
        real.traverse(relativePath, chain)
    }

    override fun traverse(relativePath: String, node: ClassNode) {
        real.traverse(relativePath, node)
    }

    override fun traverseAndroidJar(relativePath: String, chain: ClassVisitorChain) {
        real.traverseAndroidJar(relativePath, chain)
    }

    override fun traverseAndroidJar(relativePath: String, node: ClassNode) {
        real.traverseAndroidJar(relativePath, node)
    }

    override fun beforeTransform(transformer: TransformEngine) {
        executeWithStartAndFinish(
                { real.beforeTransform(transformer) },
                { GlobalMainProcessHandlerListener.startBeforeTransform(real, transformer) },
                { e -> GlobalMainProcessHandlerListener.finishBeforeTransform(real, transformer, e) }
        )
    }

    override fun transform(relativePath: String, chain: ClassVisitorChain): Boolean {
        return real.transform(relativePath, chain)
    }

    override fun transform(relativePath: String, node: ClassNode): Boolean {
        return real.transform(relativePath, node)
    }

    override fun afterTransform(transformer: TransformEngine) {
        executeWithStartAndFinish(
                { real.afterTransform(transformer) },
                { GlobalMainProcessHandlerListener.startAfterTransform(real, transformer) },
                { e -> GlobalMainProcessHandlerListener.finishAfterTransform(real, transformer, e) }
        )
    }

    override fun process(process: Process?): MutableList<FileProcessor> {
        return real.process(process)
    }

    override fun flagForClassReader(process: Process?): Int {
        return real.flagForClassReader(process)
    }

    override fun flagForClassWriter(): Int {
        return real.flagForClassWriter()
    }

    override fun needPreVerify(): Boolean {
        return real.needPreVerify()
    }

    override fun needVerify(): Boolean {
        return real.needVerify()
    }

    override fun isOnePassEnough(): Boolean {
        return real.isOnePassEnough()
    }
}