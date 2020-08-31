package com.ss.android.ugc.bytex.common.builder.internal

import com.ss.android.ugc.bytex.common.IPlugin
import org.gradle.api.Project

/**
 * Created by yangzhiqian on 2020/8/25<br/>
 */
internal class PluginStatus(
        @Transient val project: Project,
        @Transient val plugin: IPlugin) : Comparable<PluginStatus> {
    val className = plugin::class.java.name
    var name = plugin.name()
    var applyTime = 0L
    var appliedTime = 0L
    var enable = false
    var enableInDebug = false
    var alone: Boolean = false
    var isRunningAlone: Boolean = false
    var started = false
    var startTime = 0L
    var runTaskName = ""
    var runTransformClassName = ""
    var runTransformUnique = ""
    var runTransformFLow = ""
    var finished = false
    var finishTime = 0L
    var runSucceed = false
    var runFailMessage = ""

    override fun hashCode(): Int {
        return plugin.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return plugin == other
    }

    override fun compareTo(other: PluginStatus): Int {
        if (applyTime == other.applyTime) {
            return className.compareTo(other.className)
        }
        return (applyTime - other.applyTime).toInt()
    }
}