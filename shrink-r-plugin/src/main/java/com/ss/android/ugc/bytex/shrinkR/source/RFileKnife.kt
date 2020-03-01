package com.ss.android.ugc.bytex.shrinkR.source

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import groovy.util.XmlSlurper
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashSet
import kotlin.reflect.KClass

/**
 * Created by yangzhiqian on 2020-02-11<br/>
 * Desc:
 */
@PluginConfig("bytex.RFileKnife")
class RFileKnife : Plugin<Project> {
    private lateinit var extension: RFileKnifeExtension
    override fun apply(project: Project) {
        extension = project.extensions.create("RFileKnife", RFileKnifeExtension::class.java)
        project.afterEvaluate {
            project.plugins.all {
                when (it) {
                    is FeaturePlugin -> {
                        project.extensions[FeatureExtension::class].run {
                            configureRRefactor(project, featureVariants)
                            configureRRefactor(project, libraryVariants)
                        }
                    }
                    is LibraryPlugin -> {
                        project.extensions[LibraryExtension::class].run {
                            configureRRefactor(project, libraryVariants)
                        }
                    }
                    is AppPlugin -> {
                        project.extensions[AppExtension::class].run {
                            configureRRefactor(project, applicationVariants)
                        }
                    }
                }
            }
        }
    }

    private fun configureRRefactor(project: Project, variants: DomainObjectSet<out BaseVariant>) {
        val assignType = parseFormString(extension.assignType)
        val whiteList = RFileWhiteList(extension.whiteList)
        variants.all { variant ->
            val once = AtomicBoolean()
            variant.outputs.all { output ->
                if (once.compareAndSet(false, true)) {
                    val packages = HashSet<String>().apply {
                        add(getPackageName(variant))
                        extension.packageNames.forEach {
                            add(it.trim())
                        }
                    }
                    val brewResults = LinkedList<RFilesRewriter.BrewResult>()
                    output.processResources.doLast { processResources ->
                        packages.map { packageName ->
                            Schedulers.COMPUTATION().submit(RFilesRewriter().also {
                                it.rFileDir = (processResources as ProcessAndroidResources).sourceOutputDir
                                it.packageName = packageName
                                it.className = "R"
                                it.limit = extension.limitSize
                                it.verifyParse = extension.verifyParse
                                it.assignType = assignType
                                it.rBackupDir = File(project.buildDir, "RFileKnife/${variant.dirName}")
                                it.whiteList = whiteList
                            })
                        }.forEach {
                            it.get().also { result ->
                                println("RFileKnife[${result.endTime - result.startTime}]:${result.rFilePath}->${result.resultCode}")
                                brewResults.add(result)
                            }
                        }
                    }
                    project.tasks.getByName("compile${variant.name.capitalize()}JavaWithJavac").doLast {
                        brewResults.filter { it.resultCode == RFilesRewriter.BrewResult.CODE_SUCCEED }.forEach { result ->
                            File(result.backupRFilePath!!).copyTo(File(result.rFilePath), true)
                        }
                    }
                }
            }
        }
    }

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)
    }

    // Parse the variant's main manifest file in order to get the package id which is used to create
    // R.java in the right place.
    private fun getPackageName(variant: BaseVariant): String {
        val slurper = XmlSlurper(false, false)
        val list = variant.sourceSets.map { it.manifestFile }

        // According to the documentation, the earlier files in the list are meant to be overridden by the later ones.
        // So the first file in the sourceSets list should be main.
        val result = slurper.parse(list[0])
        return result.getProperty("@package").toString()
    }
}