package com.ss.android.ugc.bytex.gradletoolkit


import com.android.build.gradle.api.BaseVariant
import com.bytedance.gradle.compat.AGP
import org.gradle.api.Task

val Task.variant: BaseVariant?
    get() = this.let {
        if(!AGP.isAndroidModule(project)) return@let null
        val android = AGP.withAndroidExtension(project)
        val allVariant = android.variants
        var matchedVariant: BaseVariant? = null
        for (variant in allVariant) {
            if (it.name.endsWith(variant.name.capitalize())) {
                if (matchedVariant == null || matchedVariant.name.length < variant.name.length) {
                    matchedVariant = variant.raw()
                }
            }
        }
        matchedVariant
    }