package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import java.io.File

/**
 * The variant scope
 *
 * @author johnsonlee
 */
val BaseVariant.scope: VariantScope
    get() = variantData.scope

/**
 * The variant data
 *
 * @author johnsonlee
 */
val BaseVariant.variantData: BaseVariantData
    get() = if (this is InstallableVariantImpl) this.variantData else javaClass.getDeclaredMethod("getVariantData").invoke(this) as BaseVariantData

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