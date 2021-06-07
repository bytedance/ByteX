package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Task

val Task.variant: BaseVariant?
    get() = this.let {
        val android = project.extensions.findByName("android") as? BaseExtension ?: return@let null
        val allVariant = if (android is AppExtension) {
            android.applicationVariants.map { it as BaseVariant }
        } else if (android is LibraryExtension) {
            android.libraryVariants.map { it as BaseVariant }
        } else {
            return@let null
        }
        var matchedVariant: BaseVariant? = null
        for (variant in allVariant) {
            if (it.name.endsWith(variant.name.capitalize())) {
                if (matchedVariant == null || matchedVariant.name.length < variant.name.length) {
                    matchedVariant = variant
                }
            }
        }
        matchedVariant
    }