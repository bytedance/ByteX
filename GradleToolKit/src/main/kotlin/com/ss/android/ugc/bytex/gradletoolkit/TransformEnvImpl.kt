package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.bytedance.gradle.compat.AGP
import com.google.auto.service.AutoService
import java.io.File
import java.util.*
import com.bytedance.gradle.compat.extension.variant

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
            Artifact.AAR -> invocation!!.variant.aarArtifactCollection()
            Artifact.JAR -> invocation!!.variant.jarArtifactCollection()
            Artifact.PROCESSED_JAR -> {
                if (AGP.agpVersionCode() >= 400) {
                    invocation!!.variant.processedJarArtifactCollection()
                } else {
                    invocation!!.variant.jarArtifactCollection()
                }
            }
            Artifact.CLASSES -> invocation!!.variant.classesArtifactCollection()
            Artifact.ALL_CLASSES -> invocation!!.variant.allClasses
            Artifact.APK -> invocation!!.variant.apk
            Artifact.JAVAC -> invocation!!.variant.javacClasses
            Artifact.MERGED_ASSETS -> invocation!!.variant.mergedAssets
            Artifact.MERGED_RES -> invocation!!.variant.mergedRes
            Artifact.MERGED_MANIFESTS -> invocation!!.variant.mergedManifestFiles
            Artifact.MERGED_MANIFESTS_WITH_FEATURES -> invocation!!.variant.mergedManifestFilesWithFeatures
            Artifact.PROCESSED_RES -> invocation!!.variant.processedRes
            Artifact.SYMBOL_LIST -> invocation!!.variant.symbolList
            Artifact.SYMBOL_LIST_WITH_PACKAGE_NAME -> invocation!!.variant.symbolListWithPackageName
            Artifact.RAW_RESOURCE_SETS -> invocation!!.variant.mergeResources.computeResourceSetList0()
                    ?: emptyList()
            Artifact.RAW_ASSET_SETS -> invocation!!.variant.mergeSourceSet.assetSetList()
        }
    }
}