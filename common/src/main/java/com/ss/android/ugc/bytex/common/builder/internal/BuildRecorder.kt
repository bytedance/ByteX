package com.ss.android.ugc.bytex.common.builder.internal

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.ss.android.ugc.bytex.common.IPlugin
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import java.io.File
import java.io.FileWriter
import java.util.*

/**
 * Created by yangzhiqian on 2020/8/27<br/>
 */
internal class BuildRecorder {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private lateinit var outputDir: File
    private val transformInfoRecords = LinkedList<TransformInfoRecord>()
    fun start(project: Project) {
        outputDir = File(project.buildDir, "ByteX/build")
//        GFileUtils.deleteDirectory(outputDir)
        outputDir.mkdirs()
    }

    fun recordTransform(transformInfo: DefaultByteXBuildListener.TransformInfo, pluginStatuses: Map<IPlugin, PluginStatus>) {
        transformInfoRecords.add(TransformInfoRecord(transformInfo, pluginStatuses))
    }

    fun recordPluginStatuses(pluginStatuses: Collection<PluginStatus>) {
        val file = File(outputDir, "pluginStatuses.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
        }
        FileWriter(file).use {
            gson.toJson(pluginStatuses.sorted(), object : TypeToken<List<PluginStatus>>() {}.type, it)
        }
    }

    fun stop() {
        val file = File(outputDir, "transformInfo.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
        }
        FileWriter(file).use {
            gson.toJson(transformInfoRecords, object : TypeToken<List<TransformInfoRecord>>() {}.type, it)
        }
        transformInfoRecords.clear()
    }


    private class TransformInfoRecord(transformInfo: DefaultByteXBuildListener.TransformInfo, pluginStatuses: Map<IPlugin, PluginStatus>) {
        val name = transformInfo.transform.name
        val className = transformInfo.transform::class.java.name
        val taskName = transformInfo.getTaskName()
        var startTime = transformInfo.startTime
        var endTime = transformInfo.endTime
        var exception = transformInfo.exception?.message
        var flowInfos = transformInfo.flowInfos.values.map { TransformFlowInfoRecord(it, pluginStatuses) }.sortedBy {
            it.startRunningTime
        }

        class TransformFlowInfoRecord(flowInfo: DefaultByteXBuildListener.TransformFlowInfo, pluginStatuses: Map<IPlugin, PluginStatus>) {
            val handlers = flowInfo.handlers.map { it.value }
            val plugins = flowInfo.plugins.map { pluginStatuses[it]!! }
            var startPrepareTime = flowInfo.startPrepareTime
            var finishPrepareTime = flowInfo.finishPrepareTime
            var startRunningTime = flowInfo.startRunningTime
            var finishRunningTime = flowInfo.finishRunningTime
        }
    }
}