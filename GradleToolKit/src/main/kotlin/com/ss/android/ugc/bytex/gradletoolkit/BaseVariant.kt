package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.VariantScope
import com.bytedance.gradle.compat.extension.compatVariant
import org.gradle.api.artifacts.ArtifactCollection
import java.io.File

val BaseVariant.scope: VariantScope
    get() =  compatVariant.variantScope.originScope()

fun BaseVariant.getArtifactCollection(configType: AndroidArtifacts.ConsumedConfigType, artifactScope: AndroidArtifacts.ArtifactScope, artifactType: AndroidArtifacts.ArtifactType): ArtifactCollection {
    return compatVariant.rawArtifacts.getArtifactCollection(configType, artifactScope, artifactType)
}

val BaseVariant.blameLogOutputFolder: File
    get() = compatVariant.blameLogOutputFolder
