package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformInvocationBuilder
import com.android.build.gradle.internal.pipeline.TransformTask
import com.bytedance.gradle.compat.extension.HookContext
import com.google.common.collect.Lists
import com.ss.android.ugc.bytex.common.IPlugin
import com.ss.android.ugc.bytex.common.hook.TransformInputCompatUtil.covertToTransformInput
import com.ss.android.ugc.bytex.common.utils.ReflectionUtils
import com.ss.android.ugc.bytex.common.utils.Utils
import com.ss.android.ugc.bytex.gradletoolkit.scope
import com.ss.android.ugc.bytex.gradletoolkit.variant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LoggingManager
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap


internal sealed class HookInjector(project: Project) {
    protected val plugins = LinkedList<IPlugin>()

    init {
        project.afterEvaluate {
            try {
                val hookStatus = mutableMapOf<IPlugin, MutableList<Pair<Task, IPlugin.HookType>>>()
                for (task in project.tasks.filter {
                    //我们只能hook当前project的task
                    it.project == project
                }) {
                    hookTask(task).forEach {
                        hookStatus.computeIfAbsent(it.first) { LinkedList() }.add(Pair(task, it.second))
                    }
                }
                val msg = StringBuilder()
                msg.appendln("[ByteX HookInjector hookStatus]:")
                for (status in hookStatus) {
                    msg.append("\t").append(status.key.name()).append(":\n\t\t").append(status.value.joinToString("\n\t\t") { "${it.first.name}[${it.second.name}]" }).append("\n")
                }
                logger.info(msg.toString())
                val remind = plugins - hookStatus.keys
                if (remind.isNotEmpty()) {
                    throw IllegalStateException("[ByteX HookInjector] Plugins Can Not Hook Transform/Task:${remind.joinToString(",") { it.name() }}")
                }
            } finally {
                plugins.clear()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HookInjector::class.java)
        private val injectors = ConcurrentHashMap<Project, HookInjector>()
        fun obtain(project: Project): HookInjector {
            return injectors.computeIfAbsent(project) {
                TaskHookInjector(it).apply {
                    it.afterEvaluate {
                        injectors.remove(it)
                    }
                }
            }
        }
    }

    fun inject(plugin: IPlugin) {
        synchronized(plugins) {
            plugins.add(plugin)
        }
    }

    protected abstract fun hookTask(task: Task): Collection<Pair<IPlugin, IPlugin.HookType>>

    private open class TransformHookInjector(project: Project) : HookInjector(project) {
        override fun hookTask(task: Task): Collection<Pair<IPlugin, IPlugin.HookType>> {
            if (task !is TransformTask) {
                return emptyList()
            }
            //只能hook Before类型的
            val toHook = LinkedList<Pair<IPlugin, IPlugin.HookType>>().apply {
                plugins.filter { it.hookTask(task) == IPlugin.HookType.Before }.forEach {
                    add(Pair(it, IPlugin.HookType.Before))
                }
            }
            if (toHook.isEmpty()) {
                return emptyList()
            }
            val targetTransform = task.transform
            //todo:fix alone
            val proxyTransform = ProxyTransform(task.project, task.project.extensions.getByType(AppExtension::class.java), targetTransform.name, targetTransform).apply {
                toHook.forEach {
                    appendPlugin(it.first)
                }
            }
            ReflectionUtils.setFiled(TransformTask::class.java, task, "transform", proxyTransform)
            return toHook
        }
    }

    private class TaskHookInjector(project: Project) : TransformHookInjector(project) {
        override fun hookTask(task: Task): Collection<Pair<IPlugin, IPlugin.HookType>> {
            val hookByTransform = super.hookTask(task)
            val toHook = LinkedList<Pair<IPlugin, IPlugin.HookType>>().apply {
                plugins.forEach {
                    val hookType = it.hookTask(task)
                    if (hookType != IPlugin.HookType.None) {
                        add(Pair(it, hookType))
                    }
                }
                //移除TransformHookInjector hook的插件状态
                removeAll(hookByTransform)
            }
            if (toHook.isEmpty()) {
                return hookByTransform
            }
            val hookBefore = toHook.filter { it.second == IPlugin.HookType.Before }
            val hookAfter = toHook.filter { it.second == IPlugin.HookType.After }
            //先查找一下当前前task是来自哪个变体，需要防范找不到的情况(throw exception?)
            val android = task.project.extensions.getByType(AppExtension::class.java)
            if (hookBefore.isNotEmpty()) {
                task.doFirst {
                    actionTransform(it, covertToTransformInput(it.inputs.files.files), hookBefore.map { it.first }, android, IPlugin.HookType.Before)
                }
            }
            if (hookAfter.isNotEmpty()) {
                task.doLast {
                    actionTransform(it, covertToTransformInput(it.outputs.files.files), hookAfter.map { it.first }, android, IPlugin.HookType.After)
                }
            }
            return hookBefore + hookAfter + hookByTransform
        }

        private fun actionTransform(task: Task, inputs: Collection<File>, toHook: Collection<IPlugin>,
                                    android: AppExtension, hookType: IPlugin.HookType) {
            //创建一个加的Transform对象模拟代理
            val fakeTransform = FakeTransform(
                    hookType.name + if (task is TransformTask) {
                        task.transform.name.capitalize()
                    } else {
                        task.name.capitalize().replace(task.variant?.name?.capitalize()
                                ?: "", "")
                    },
                    toHook.flatMap { it.transformConfiguration().inputTypes }.toSet(),
                    toHook.flatMap { it.transformConfiguration().getScopes(task.variant?.scope) }.toMutableSet()
            )
            val proxyTransform = ProxyTransform(task.project, android, fakeTransform.name, fakeTransform).apply {
                toHook.forEach {
                    appendPlugin(it)
                }
            }
            //模拟创建Transform的输入对象用于构建TransformInvocation
            val jarInputs: MutableList<JarInput> = Lists.newArrayList()
            val directoryInputs: MutableList<DirectoryInput> = Lists.newArrayList()
            //todo fixme :input files are irregular->filter non-project input files.
            val projectDirPath = task.project.rootProject.projectDir.absolutePath
            inputs.filter { !it.absolutePath.startsWith(projectDirPath) }.forEach {
                logger.warn("[ByteX HookInjector]${fakeTransform.name} skip non-project input:${it.absolutePath}")
            }
            for (input in inputs.filter { it.absolutePath.startsWith(projectDirPath) }) {
                if (input.exists()) {
                    if (input.isDirectory) {
                        logger.warn("[ByteX HookInjector]${fakeTransform.name} directory input:${input.absolutePath}")
                        directoryInputs.add(
                                DirectoryInputImpl(
                                        input.absolutePath,
                                        input,
                                        fakeTransform.inputTypes,
                                        fakeTransform.scopes))
                    } else if (input.name.endsWith(".jar")) {
                        logger.warn("[ByteX HookInjector]${fakeTransform.name} jar input:${input.absolutePath}")
                        jarInputs.add(
                                JarInputImpl(
                                        input.absolutePath,
                                        input,
                                        Status.NOTCHANGED,
                                        fakeTransform.inputTypes,
                                        fakeTransform.scopes))
                    } else {
                        logger.warn("[ByteX HookInjector]${fakeTransform.name} skip unknown input:${input.absolutePath}")
                    }
                }
            }
            //模拟创建Transform的context对象用于构建TransformInvocation
            val context = object : HookContext {
                //gradle compat 需要反射字段获取 task ，如果被 hook 了就会报错，这里直接让 compat 库
                //暴露一个接口，让它主动查询这是个 hook 的类，然后直接获取 task 而不是反射
                override val hookTask: Task = task
                //for context.project
                val project = task.project
                override fun getPath(): String = task.path
                override fun getWorkerExecutor(): WorkerExecutor? = (task.project as ProjectInternal).services.get(WorkerExecutor::class.java)
                override fun getTemporaryDir(): File = task.temporaryDir
                override fun getProjectName(): String = task.project.name
                override fun getVariantName(): String = task.variant?.name ?: ""
                override fun getLogging(): LoggingManager = task.logging
            }
            val transformOutputProvider = object : TransformOutputProvider {
                override fun getContentLocation(name: String, types: MutableSet<QualifiedContent.ContentType>, scopes: MutableSet<in QualifiedContent.Scope>, format: Format): File {
                    //对于Hook方式，我们原样覆盖，如果name不是来自之前的输入的absolutePath不合法，我们抛一个异常出去
                    return inputs.firstOrNull { it.absolutePath == name }
                            ?: throw IllegalArgumentException("Can Not getContentLocation From Name:${name}")
                }

                override fun deleteAll() {
                    //do nothing
                }
            }
            //模拟调用
            proxyTransform.transform(TransformInvocationBuilder(context)
                    .addInputs(listOf(TransformInputImpl(jarInputs, directoryInputs)))
                    .addReferencedInputs(emptyList())
                    .addSecondaryInputs(emptyList())
                    .addOutputProvider(transformOutputProvider)
                    .setIncrementalMode(false)
                    .build())
        }
    }
}
