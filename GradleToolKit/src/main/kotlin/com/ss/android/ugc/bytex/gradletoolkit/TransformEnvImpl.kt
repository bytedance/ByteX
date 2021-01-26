package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.google.auto.service.AutoService
import java.io.File
import java.util.*
import kotlin.streams.asStream
import kotlin.streams.toList

/**
 * Created by tanlehua on 2019-04-29.
 */
@AutoService(TransformEnv::class)
class TransformEnvImpl() : TransformEnv {
    private var invocation: TransformInvocation? = null

    override fun setTransformInvocation(invocation: TransformInvocation) {
        this.invocation = invocation
    }

    override fun getArtifact(artifact: Artifact): Collection<File> {
        if (invocation == null) {
            return Collections.emptyList()
        }
        return when (artifact) {
            Artifact.AAR -> {
                invocation!!.variant.getArtifactCollection(
                        AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        AndroidArtifacts.ArtifactType.AAR
                ).artifactFiles.files
            }
            Artifact.JAR -> {
                invocation!!.variant.getArtifactCollection(
                        AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        AndroidArtifacts.ArtifactType.JAR
                ).artifactFiles.files
            }
            Artifact.PROCESSED_JAR -> {
                invocation!!.variant.getArtifactCollection(
                        AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        if (ANDROID_GRADLE_PLUGIN_VERSION.major > 3 || ANDROID_GRADLE_PLUGIN_VERSION.minor >= 2) {
                            AndroidArtifacts.ArtifactType.PROCESSED_JAR
                        } else {
                            AndroidArtifacts.ArtifactType.JAR
                        }
                ).artifactFiles.files
            }
            Artifact.CLASSES -> {
                invocation!!.variant.getArtifactCollection(
                        AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        AndroidArtifacts.ArtifactType.CLASSES
                ).artifactFiles.files
            }
            Artifact.ALL_CLASSES -> invocation!!.variant.bridgeAllClass
            Artifact.APK -> invocation!!.variant.bridgeApk
            Artifact.JAVAC ->
                if (ANDROID_GRADLE_PLUGIN_VERSION.major == 3 && ANDROID_GRADLE_PLUGIN_VERSION.minor >= 2 && ANDROID_GRADLE_PLUGIN_VERSION.minor <= 5) {
                    invocation!!.variant.getArtifactFiles(InternalArtifactType.JAVAC)
                } else {
                    emptyList()
                }
            Artifact.MERGED_ASSETS -> invocation!!.variant.bridgeMergedAssets
            Artifact.MERGED_RES -> invocation!!.variant.bridgeMergedRes
            Artifact.MERGED_MANIFESTS -> invocation!!.variant.bridgeMergedManifests.flatMap {
                when {
                    it.isDirectory ->
                        it.walk().asStream().filter { it.isFile }.toList()
                    it.isFile -> listOf(it)
                    else -> emptyList()
                }.filter { file ->
                    file.isFile && file.name.endsWith(".xml")
                }
            }
            Artifact.MERGED_MANIFESTS_WITH_FEATURES ->
                invocation!!.project.rootProject.subprojects
                        .mapNotNull { it.extensions.findByName("android") as? AppExtension? }
                        .map {
                            var result: ApplicationVariant? = null
                            it.applicationVariants.forEach {
                                if (invocation!!.context.variantName.contains(it.name) && (result == null || it.name.contains(result!!.name))) {
                                    result = it
                                }
                            }
                            result!!
                        }
                        .flatMap {
                            it.bridgeMergedManifests
                        }
                        .flatMap {
                            when {
                                it.isDirectory -> it.walk().asStream().filter { it.isFile }.toList()
                                it.isFile -> listOf(it)
                                else -> emptyList()
                            }
                        }.filter { it ->
                            it.isFile && it.name.endsWith(".xml")
                        }.toList()
            Artifact.PROCESSED_RES -> invocation!!.variant.bridgeMergedProcessedRes
            Artifact.SYMBOL_LIST -> invocation!!.variant.bridgeSymbolList
            Artifact.SYMBOL_LIST_WITH_PACKAGE_NAME -> invocation!!.variant.bridgeSymbolListWithPackageName
            Artifact.RAW_RESOURCE_SETS -> invocation!!.variant.mergeResources.resourceSetList()
            Artifact.RAW_ASSET_SETS -> invocation!!.variant.mergeAssets.assetSetList()
        }
    }
}