package com.ss.android.ugc.bytex.refercheck

import com.android.build.gradle.AppExtension
import com.ss.android.ugc.bytex.common.configuration.ProjectOptions
import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig
import com.ss.android.ugc.bytex.refercheck.cli.QuickReferCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project

@PluginConfig("bytex.quick_refer_check")
class QuickReferCheckPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        ProjectOptions.INSTANCE.init(project)
        GlobalWhiteListManager.INSTANCE.init(project)
        val extension = project.extensions.create("quick_refer_check", ReferCheckExtension::class.java)
        project.afterEvaluate {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.applicationVariants.forEach {
                project.tasks.register("processQuickReferCheckWith${it.name.capitalize()}", QuickReferCheckTask::class.java, project, android, extension, it)
            }
        }
    }
}