package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.didiglobal.booster.gradle.variantData
import java.io.File

/**
 * get a list of merged resource sets.
 */
val BaseVariant.resourceSetList: List<File>
    get() = this.mergeResources.resourceSetList()

/**
 * get a list of merged asset sets.
 */
val BaseVariant.assetSetList: List<File>
    get() = this.mergeAssets.assetSetList()

val BaseVariant.scope: VariantScope
    get() = variantData.scope