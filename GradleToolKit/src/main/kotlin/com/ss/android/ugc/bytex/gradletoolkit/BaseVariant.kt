package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.api.BaseVariant
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