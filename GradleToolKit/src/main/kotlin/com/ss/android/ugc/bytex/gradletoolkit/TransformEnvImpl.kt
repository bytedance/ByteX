package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.google.auto.service.AutoService
import org.gradle.api.UnknownDomainObjectException
import java.io.File
import java.util.*
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
            Artifact.AAR -> invocation!!.variant.scope.getArtifactCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH, AndroidArtifacts.ArtifactScope.ALL, AndroidArtifacts.ArtifactType.AAR).artifactFiles.files
            Artifact.ALL_CLASSES -> invocation!!.variant.scope.allClasses
            Artifact.APK -> invocation!!.variant.scope.apk
            Artifact.JAR -> invocation!!.variant.scope.getArtifactCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH, AndroidArtifacts.ArtifactScope.ALL, AndroidArtifacts.ArtifactType.JAR).artifactFiles.files
            Artifact.JAVAC -> invocation!!.variant.scope.javac
            Artifact.MERGED_ASSETS -> invocation!!.variant.scope.mergedAssets
            Artifact.MERGED_RES -> invocation!!.variant.scope.mergedRes
            Artifact.MERGED_MANIFESTS -> invocation!!.variant.scope.mergedManifests.flatMap {
                return when {
                    it.isDirectory -> it.listFiles().toList()
                    it.isFile -> listOf(it)
                    else -> emptyList()
                }.filter { file ->
                    file.isFile && file.name.endsWith(".xml")
                }
            }
            Artifact.MERGED_MANIFESTS_WITH_FEATURES -> invocation!!.project.rootProject.subprojects.stream()
                    .map {
                        //过滤出所有的app和feature的project
                        try {
                            it.getAndroid<BaseExtension>()
                        } catch (e: UnknownDomainObjectException) {
                            null
                        } as? AppExtension
                    }
                    .filter {
                        it != null
                    }
                    .map {
                        var result: ApplicationVariant? = null
                        it!!.applicationVariants.forEach {
                            if (invocation!!.context.variantName.contains(it.name) && (result == null || it.name.contains(result!!.name))) {
                                result = it
                            }
                        }
                        result!!
                    }
                    .map {
                        it.scope
                    }
                    .flatMap {
                        it.mergedManifests.stream()
                    }
                    .map {
                        when {
                            it.isDirectory -> it.listFiles().toList()
                            it.isFile -> listOf(it)
                            else -> emptyList()
                        }
                    }
                    .flatMap { it.stream() }
                    .filter { file ->
                        file.isFile && file.name.endsWith(".xml")
                    }.toList()
            Artifact.PROCESSED_RES -> invocation!!.variant.scope.processedRes
            Artifact.SYMBOL_LIST -> invocation!!.variant.scope.symbolList
            Artifact.SYMBOL_LIST_WITH_PACKAGE_NAME -> invocation!!.variant.scope.symbolListWithPackageName
            Artifact.RAW_RESOURCE_SETS -> invocation!!.variant.resourceSetList
            Artifact.RAW_ASSET_SETS -> invocation!!.variant.assetSetList
        }
    }
}