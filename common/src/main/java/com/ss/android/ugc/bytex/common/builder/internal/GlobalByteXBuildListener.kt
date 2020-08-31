package com.ss.android.ugc.bytex.common.builder.internal

import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.ss.android.ugc.bytex.common.IPlugin
import com.ss.android.ugc.bytex.common.builder.ByteXBuildListener
import com.ss.android.ugc.bytex.common.builder.ByteXBuildListenerManager
import com.ss.android.ugc.bytex.common.flow.TransformFlow
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 */
internal object GlobalByteXBuildListener : ByteXBuildListener {
    private var project: Project? = null

    override fun onByteXPluginApply(project: Project, plugin: IPlugin) {
        //todo:deal with bytex plugins enabled in multi modules
        if (project != GlobalByteXBuildListener.project) {
            if (GlobalByteXBuildListener.project != null) {
                System.err.println("${GlobalByteXBuildListener.project!!.path} had applied ByteX Plugins Before,Is There Any Unexpected Situation!!!")
            }
            GlobalByteXBuildListener.project?.gradle?.removeListener(GradleBuildListener)
            GlobalByteXBuildListener.project = project
            project.gradle.addBuildListener(GradleBuildListener)
        }
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginApply(project, plugin)
        }
    }

    override fun onByteXPluginApplied(plugin: IPlugin) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginApplied(plugin)
        }
    }


    override fun onProjectBuildStart(project: Project) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onProjectBuildStart(project)
        }
    }

    override fun onByteXPluginTransformStart(transform: Transform, transformInvocation: TransformInvocation) {
        val transformInvocationProxy = object : TransformInvocation by transformInvocation {
            override fun getOutputProvider(): TransformOutputProvider {
                throw UnsupportedOperationException()
            }
        }
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginTransformStart(transform, transformInvocationProxy)
        }
    }

    override fun onByteXPluginStart(plugin: IPlugin) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginStart(plugin)
        }
    }

    override fun onAppendMainProcessHandler(transformFlow: TransformFlow, handler: MainProcessHandler) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onAppendMainProcessHandler(transformFlow, handler)
        }
    }

    override fun startPrepare(transformFlow: TransformFlow) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.startPrepare(transformFlow)
        }
    }

    override fun finishPrepare(transformFlow: TransformFlow, exception: Exception?) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.finishPrepare(transformFlow, exception)
        }
    }

    override fun startRunning(transformFlow: TransformFlow, isIncremental: Boolean) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.startRunning(transformFlow, isIncremental)
        }
    }

    override fun finishRunning(transformFlow: TransformFlow, exception: Exception?) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.finishRunning(transformFlow, exception)
        }
    }

    override fun onByteXPluginFinished(plugin: IPlugin) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginFinished(plugin)
        }
    }

    override fun onByteXPluginTransformFinished(transform: Transform, exception: Exception?) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onByteXPluginTransformFinished(transform, exception)
        }
    }

    override fun onProjectBuildFinished(p: Project, throwable: Throwable?) {
        for (listener in ByteXBuildListenerManager.getByteXBuildListeners()) {
            listener.onProjectBuildFinished(p, throwable)
        }
        p.gradle.removeListener(GradleBuildListener)
        project = null
    }

    object GradleBuildListener : BuildAdapter() {
        override fun projectsEvaluated(gradle: Gradle) {
            onProjectBuildStart(project!!)
        }

        override fun buildFinished(result: BuildResult) {
            onProjectBuildFinished(project!!, result.failure)
        }
    }
}