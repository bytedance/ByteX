package com.ss.android.ugc.bytex.gradletoolkit




import com.android.build.gradle.internal.scope.VariantScope
import com.android.builder.model.Version
import com.android.repository.Revision
import com.bytedance.gradle.compat.AGP

import org.gradle.api.Project

//todo:fix me
val revision = Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION)
fun Project.findVariantScope(variantName: String): VariantScope? {
    return AGP.withAndroidPlugin(this).variantManager.variants.firstOrNull { it.name==variantName }?.originScope()
}