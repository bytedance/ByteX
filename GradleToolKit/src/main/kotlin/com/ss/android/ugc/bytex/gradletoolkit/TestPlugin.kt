package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.lang.RuntimeException

class TestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val android = target.extensions.findByName("android") as AppExtension
        android.registerTransform(TestTransform(target))
    }

    class TestTransform(val project: Project) : Transform() {
        override fun getName(): String = "test"

        override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> = TransformManager.CONTENT_CLASS

        override fun isIncremental(): Boolean = false

        val env = TransformEnvImpl()
        override fun transform(transformInvocation: TransformInvocation) {
            super.transform(transformInvocation)
            env.setTransformInvocation(transformInvocation)
            val msg = StringBuilder()
            project.findVariantScope(transformInvocation.context.variantName)
            msg.appendln(printOutput(Artifact.AAR))
            msg.appendln(printOutput(Artifact.ALL_CLASSES))
            msg.appendln(printOutput(Artifact.APK))
            msg.appendln(printOutput(Artifact.JAR))
            msg.appendln(printOutput(Artifact.PROCESSED_JAR))
            msg.appendln(printOutput(Artifact.CLASSES))
            msg.appendln(printOutput(Artifact.JAVAC))
            msg.appendln(printOutput(Artifact.MERGED_ASSETS))
            msg.appendln(printOutput(Artifact.MERGED_RES))
            msg.appendln(printOutput(Artifact.MERGED_MANIFESTS))
            msg.appendln(printOutput(Artifact.MERGED_MANIFESTS_WITH_FEATURES))
            msg.appendln(printOutput(Artifact.PROCESSED_RES))
            msg.appendln(printOutput(Artifact.SYMBOL_LIST))
            msg.appendln(printOutput(Artifact.SYMBOL_LIST_WITH_PACKAGE_NAME))
            msg.appendln(printOutput(Artifact.RAW_RESOURCE_SETS))
            msg.appendln(printOutput(Artifact.RAW_ASSET_SETS))
            throw RuntimeException("\n" + msg.toString())
        }

        private fun printOutput(artifact: Artifact): String {
            return artifact.name + ":\n\t" + env.getArtifact(artifact).map { it.absolutePath }.sorted().joinToString("\n\t")
        }

        override fun getScopes(): MutableSet<in QualifiedContent.Scope> = TransformManager.SCOPE_FULL_PROJECT
    }
}