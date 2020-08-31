package com.ss.android.ugc.bytex.common.builder.internal

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformTask
import com.google.common.collect.Sets
import com.ss.android.ugc.bytex.common.IPlugin
import com.ss.android.ugc.bytex.common.builder.ByteXBuildListener
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.flow.TransformFlow
import com.ss.android.ugc.bytex.common.flow.main.LifecycleAwareManProcessHandler
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandlerListener
import com.ss.android.ugc.bytex.transformer.TransformEngine
import org.gradle.api.Project
import org.gradle.api.Task
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 */
internal object DefaultByteXBuildListener : ByteXBuildListener, MainProcessHandlerListener {
    private var project: Project? = null
    private var pluginStatuses = ConcurrentHashMap<IPlugin, PluginStatus>()
    private var currentTransformInfo: TransformInfo? = null
    private var buildRecorder = BuildRecorder()
    private var validRun = false

    override fun onByteXPluginApply(project: Project, plugin: IPlugin) {
        //todo:deal with bytex plugins enabled in multi modules
        DefaultByteXBuildListener.project = project
        validRun = false
        if (!plugin.transformConfiguration().isIncremental) {
            System.err.println("[ByteX Warning]:" + this.javaClass.name + " does not yet support incremental build")
        }
        PluginStatus(project, plugin).apply {
            pluginStatuses[plugin] = this
            applyTime = System.currentTimeMillis()
        }
    }

    override fun onByteXPluginApplied(plugin: IPlugin) {
        pluginStatuses[plugin]!!.appliedTime = System.currentTimeMillis()
    }


    override fun onProjectBuildStart(project: Project) {
        buildRecorder.start(project)
        if (BooleanProperty.CHECK_INCREMENTAL_INDEBUG.value()) {
            for (plugin in pluginStatuses.keys()) {
                check(!(!plugin.transformConfiguration().isIncremental && plugin.extension.isEnableInDebug)) { "ByteX plugin " + plugin.name() + " does not support incremental" }
            }
        }
        for (value in pluginStatuses.values) {
            value.name = value.plugin.name()
            value.enable = value.plugin.extension.isEnable
            value.enableInDebug = value.plugin.extension.isEnableInDebug
            value.alone = value.plugin.alone()
            value.isRunningAlone = value.plugin.isRunningAlone
        }
    }

    override fun onByteXPluginTransformStart(transform: Transform, transformInvocation: TransformInvocation) {
        validRun = true
        currentTransformInfo = TransformInfo(transform, transformInvocation)
        currentTransformInfo!!.task = project!!.tasks.filter {
            //此处不使用==是因为有些操作(比如构建优化或者监控等)会代理替换Transform
            //此处实际上是对比了taskName，因为名字不会重复
            it is TransformTask &&
                    it.transform.name == transform.name &&
                    Sets.difference(it.transform.inputTypes, transform.inputTypes).isEmpty()
        }.firstOrNull()
        currentTransformInfo!!.startTime = System.currentTimeMillis()
    }

    override fun onByteXPluginStart(plugin: IPlugin) {
        pluginStatuses[plugin]!!.started = true
        pluginStatuses[plugin]!!.startTime = System.currentTimeMillis()
        pluginStatuses[plugin]!!.runTaskName = currentTransformInfo!!.getTaskName()
        pluginStatuses[plugin]!!.runTransformClassName = currentTransformInfo!!.transform::class.java.name
        pluginStatuses[plugin]!!.runTransformUnique = currentTransformInfo!!.transform.name
        plugin.transformFlow?.let {
            currentTransformInfo!!.flowInfos.computeIfAbsent(it) {
                TransformFlowInfo(it)
            }.apply {
                plugins.add(plugin)
            }
        }

    }

    override fun onAppendMainProcessHandler(transformFlow: TransformFlow, handler: MainProcessHandler) {
        currentTransformInfo!!.flowInfos.computeIfAbsent(transformFlow) {
            TransformFlowInfo(transformFlow)
        }.apply {
            handlers[handler] = MainProcessHandlerInfo(handler)
            if (handler is IPlugin) {
                plugins.add(handler)
            } else if (handler is LifecycleAwareManProcessHandler && handler.real is IPlugin) {
                plugins.add(handler.real)
            }
        }
    }

    override fun startPrepare(transformFlow: TransformFlow) {
        currentTransformInfo!!.flowInfos.computeIfAbsent(transformFlow) {
            TransformFlowInfo(transformFlow)
        }.apply {
            startPrepareTime = System.currentTimeMillis()
        }
    }

    override fun finishPrepare(transformFlow: TransformFlow, exception: Exception?) {
        currentTransformInfo!!.flowInfos.computeIfAbsent(transformFlow) {
            TransformFlowInfo(transformFlow)
        }.apply {
            finishPrepareTime = System.currentTimeMillis()
            if (exception != null) {
                for (iPlugin in plugins) {
                    pluginStatuses[iPlugin]!!.runSucceed = false
                    pluginStatuses[iPlugin]!!.runFailMessage = exception.message.toString()
                }
            }
        }
    }

    override fun startRunning(transformFlow: TransformFlow, isIncremental: Boolean) {
        currentTransformInfo!!.flowInfos.computeIfAbsent(transformFlow) {
            TransformFlowInfo(transformFlow)
        }.apply {
            startRunningTime = System.currentTimeMillis()
        }
    }

    override fun finishRunning(transformFlow: TransformFlow, exception: Exception?) {
        currentTransformInfo!!.flowInfos.computeIfAbsent(transformFlow) {
            TransformFlowInfo(transformFlow)
        }.apply {
            finishRunningTime = System.currentTimeMillis()
            for (iPlugin in plugins) {
                pluginStatuses[iPlugin]!!.runSucceed = exception == null
                pluginStatuses[iPlugin]!!.runFailMessage = exception?.message.toString()
            }
        }
    }

    override fun onByteXPluginFinished(plugin: IPlugin) {
        plugin.transformFlow?.let {
            currentTransformInfo!!.flowInfos.computeIfAbsent(it) {
                TransformFlowInfo(it)
            }.apply {
                plugins.add(plugin)
            }
        }
        pluginStatuses[plugin]!!.runTransformFLow = plugin.transformFlow?.name()
                ?: currentTransformInfo!!.flowInfos
                        .values
                        .firstOrNull {
                            plugin in it.plugins
                        }?.transformFlow?.name() ?: "Unknown"
        pluginStatuses[plugin]!!.finished = true
        pluginStatuses[plugin]!!.finishTime = System.currentTimeMillis()
    }

    override fun onByteXPluginTransformFinished(transform: Transform, exception: Exception?) {
        currentTransformInfo!!.exception = exception
        currentTransformInfo!!.endTime = System.currentTimeMillis()
        buildRecorder.recordTransform(currentTransformInfo!!, pluginStatuses)
        currentTransformInfo = null
    }

    override fun onProjectBuildFinished(p: Project, throwable: Throwable?) {
        if (validRun) {
            buildRecorder.recordPluginStatuses(pluginStatuses.values)
            buildRecorder.stop()
        }
        pluginStatuses.clear()
        currentTransformInfo = null
        project = null
        validRun = false
    }

    //============MainProcessHandlerListener Start================

    private fun findMainProcessHandlerInfo(handler: MainProcessHandler) =
            currentTransformInfo!!.flowInfos.values.map {
                it.handlers[handler]
            }.filterNotNull()

    override fun startInit(handler: MainProcessHandler, transformer: TransformEngine) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.startInitTime = System.currentTimeMillis()
        }
    }

    override fun finishInit(handler: MainProcessHandler, transformer: TransformEngine, exception: java.lang.Exception?) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.finishInitTime = System.currentTimeMillis()
            handlerInfo.initSucceed = exception == null
        }
    }

    override fun startTraverseIncremental(handlers: MutableCollection<MainProcessHandler>) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.startTraverseIncrementalTime = System.currentTimeMillis()
            }
        }
    }

    override fun finishTraverseIncremental(handlers: MutableCollection<MainProcessHandler>, exception: java.lang.Exception?) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.finishTraverseIncrementalTime = System.currentTimeMillis()
                handlerInfo.traverseIncrementalSucceed = exception == null
            }
        }
    }

    override fun startBeforeTraverse(handler: MainProcessHandler, transformer: TransformEngine) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.startBeforeTraverseTime = System.currentTimeMillis()
        }
    }

    override fun finishBeforeTraverse(handler: MainProcessHandler, transformer: TransformEngine, exception: java.lang.Exception?) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.finishBeforeTraverseTime = System.currentTimeMillis()
            handlerInfo.beforeTraverseSucceed = exception == null
        }
    }

    override fun startStartRunning(handler: MainProcessHandler, transformer: TransformEngine) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.startStartRunningTime = System.currentTimeMillis()
        }
    }

    override fun finishStartRunning(handler: MainProcessHandler, transformer: TransformEngine, exception: java.lang.Exception?) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.finishStartRunningTime = System.currentTimeMillis()
            handlerInfo.startRunningSucceed = exception == null
        }
    }

    override fun startTraverse(handlers: MutableCollection<MainProcessHandler>) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.startTraverseTime = System.currentTimeMillis()
            }
        }
    }

    override fun finishTraverse(handlers: MutableCollection<MainProcessHandler>, exception: java.lang.Exception?) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.finishTraverseTime = System.currentTimeMillis()
                handlerInfo.traverseSucceed = exception == null
            }
        }
    }

    override fun startTraverseAndroidJar(handlers: MutableCollection<MainProcessHandler>) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.startTraverseAndroidJarTime = System.currentTimeMillis()
            }
        }
    }

    override fun finishTraverseAndroidJar(handlers: MutableCollection<MainProcessHandler>, exception: java.lang.Exception?) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.finishTraverseAndroidJarTime = System.currentTimeMillis()
                handlerInfo.traverseAndroidJarSucceed = exception == null
            }
        }
    }

    override fun startBeforeTransform(handler: MainProcessHandler, transformer: TransformEngine) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.startBeforeTransformTime = System.currentTimeMillis()
        }
    }

    override fun finishBeforeTransform(handler: MainProcessHandler, transformer: TransformEngine, exception: java.lang.Exception?) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.finishBeforeTransformTime = System.currentTimeMillis()
            handlerInfo.beforeTransformSucceed = exception == null
        }
    }

    override fun startTransform(handlers: MutableCollection<MainProcessHandler>) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.startTransformTime = System.currentTimeMillis()
            }
        }
    }

    override fun finishTransform(handlers: MutableCollection<MainProcessHandler>, exception: java.lang.Exception?) {
        for (handler in handlers) {
            for (handlerInfo in findMainProcessHandlerInfo(handler)) {
                handlerInfo.finishTransformTime = System.currentTimeMillis()
                handlerInfo.transformSucceed = exception == null
            }
        }
    }

    override fun startAfterTransform(handler: MainProcessHandler, transformer: TransformEngine) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.startAfterTransformTime = System.currentTimeMillis()
        }
    }

    override fun finishAfterTransform(handler: MainProcessHandler, transformer: TransformEngine, exception: java.lang.Exception?) {
        for (handlerInfo in findMainProcessHandlerInfo(handler)) {
            handlerInfo.finishAfterTransformTime = System.currentTimeMillis()
            handlerInfo.afterTransformSucceed = exception == null
        }
    }


    //============MainProcessHandlerListener End================

    class TransformInfo(val transform: Transform, val transformInvocation: TransformInvocation) {
        var task: Task? = null
        var startTime = 0L
        var flowInfos = ConcurrentHashMap<TransformFlow, TransformFlowInfo>()
        var endTime = 0L
        var exception: Exception? = null

        fun getTaskName(): String {
            if (task != null) {
                return task!!.path
            }
            val types = mutableListOf<String>()
            if (transform.inputTypes.contains(QualifiedContent.DefaultContentType.CLASSES)) {
                types.add("Classes")
            }
            if (transform.inputTypes.contains(QualifiedContent.DefaultContentType.RESOURCES)) {
                types.add("Resources")
            }
            return "transform${types.joinToString("And")}With${transform.name.capitalize()}${transformInvocation.context.variantName.capitalize()}"
        }
    }

    class TransformFlowInfo(val transformFlow: TransformFlow) {
        val handlers = ConcurrentHashMap<MainProcessHandler, MainProcessHandlerInfo>()
        val plugins = Collections.synchronizedList(object : LinkedList<IPlugin>() {
            override fun add(element: IPlugin): Boolean {
                if (contains(element)) {
                    return false
                }
                return super.add(element)
            }
        })
        var startPrepareTime = 0L
        var finishPrepareTime = 0L
        var startRunningTime = 0L
        var finishRunningTime = 0L
    }

    class MainProcessHandlerInfo(handler: MainProcessHandler) {
        val name = if (handler is LifecycleAwareManProcessHandler) {
            handler.real
        } else {
            handler
        }::class.java.name

        var startInitTime = 0L
        var finishInitTime = 0L
        var initSucceed = false

        var startTraverseIncrementalTime = 0L
        var finishTraverseIncrementalTime = 0L
        var traverseIncrementalSucceed = false

        var startBeforeTraverseTime = 0L
        var finishBeforeTraverseTime = 0L
        var beforeTraverseSucceed = false

        var startStartRunningTime = 0L
        var finishStartRunningTime = 0L
        var startRunningSucceed = false

        var startTraverseTime = 0L
        var finishTraverseTime = 0L
        var traverseSucceed = false

        var startTraverseAndroidJarTime = 0L
        var finishTraverseAndroidJarTime = 0L
        var traverseAndroidJarSucceed = false

        var startBeforeTransformTime = 0L
        var finishBeforeTransformTime = 0L
        var beforeTransformSucceed = false

        var startTransformTime = 0L
        var finishTransformTime = 0L
        var transformSucceed = false

        var startAfterTransformTime = 0L
        var finishAfterTransformTime = 0L
        var afterTransformSucceed = false
    }
}