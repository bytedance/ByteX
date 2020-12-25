package com.ss.android.ugc.bytex.refercheck.cli

import com.android.build.api.transform.Context
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformInvocationBuilder
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.utils.Utils
import com.ss.android.ugc.bytex.common.white_list.WhiteList
import com.ss.android.ugc.bytex.gradletoolkit.Artifact
import com.ss.android.ugc.bytex.refercheck.ReferCheckContext
import com.ss.android.ugc.bytex.refercheck.ReferCheckExtension
import com.ss.android.ugc.bytex.refercheck.log.ErrorLogGenerator
import com.ss.android.ugc.bytex.refercheck.log.TipsProvider
import com.ss.android.ugc.bytex.transformer.TransformContext
import com.ss.android.ugc.bytex.transformer.TransformOptions
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.util.*
import javax.inject.Inject

open class QuickReferCheckTask @Inject constructor(val pj: Project, val android: AppExtension, val extension: ReferCheckExtension, val variant: BaseVariant) : DefaultTask(), Context {
    private val whiteList by lazy {
        WhiteList().apply {
            LinkedList<String>().let {
                extension.whiteList?.apply {
                    it.addAll(this)
                }
                (pj.extensions.findByName("refer_check") as? ReferCheckExtension)?.whiteList?.apply {
                    it.addAll(this)
                }
                initWithWhiteList(it)
            }
            ReferCheckContext.appendCommonWhiteList(this)
        }
    }

    @TaskAction
    fun doTaskAction() {
        if (!extension.isEnable) {
            return
        }
        val context = TransformContext(
                TransformInvocationBuilder(this)
                        .addInputs(emptyList())
                        .addReferencedInputs(emptyList())
                        .addSecondaryInputs(emptyList())
                        .addOutputProvider(null)
                        .setIncrementalMode(false)
                        .build(),
                pj,
                android,
                TransformOptions.Builder()
                        .setPluginIncremental(false)
                        .setShouldSaveCache(false)
                        .setUseFixedTimestamp(false)
                        .setUseRawCache(false)
                        .setForbidUseLenientMutationDuringGetArtifact(BooleanProperty.FORBID_USE_LENIENT_MUTATION_DURING_GET_ARTIFACT.value())
                        .build())
        if (!extension.isEnableInDebug && !context.isReleaseBuild) {
            return
        }
        Main.checkReference(context.getArtifact(Artifact.CLASSES), listOf(context.androidJar()), whiteList).apply {
            val startTime = System.currentTimeMillis()
            if (keepByWhiteList.isNotEmpty() && extension.isPrintKept) {
                println("keepByWhiteList:\n\t" + ErrorLogGenerator(
                        null,
                        TipsProvider { fileName: String -> Utils.getAllFileCachePath(context, fileName) },
                        variantName,
                        null,
                        keepByWhiteList
                ).generate())
            }
            println("======================================================")
            if (inaccessibleNodes.isNotEmpty()) {
                ErrorLogGenerator(
                        null,
                        TipsProvider { fileName: String -> Utils.getAllFileCachePath(context, fileName) },
                        variantName,
                        null,
                        inaccessibleNodes
                ).generate().apply {
                    System.err.println("inaccessibleNodes:\n\t$this")
                    println("[QuickReferCheck] Quick Refer Check Fail!![${System.currentTimeMillis() - startTime}ms]")
                    if (extension.isStrictMode) {
                        throw RuntimeException(this)
                    }
                }
            } else {
                println("[QuickReferCheck] Quick Refer Check Succeed!![${System.currentTimeMillis() - startTime}ms]")
            }
        }
    }

    override fun getWorkerExecutor(): WorkerExecutor = (pj as ProjectInternal).services.get<WorkerExecutor>(WorkerExecutor::class.java)

    override fun getProjectName(): String = pj.name

    override fun getVariantName(): String = variant.name
}