package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.scope.VariantScope
import com.android.builder.model.Version
import com.android.repository.Revision
import org.gradle.api.Project

//todo:fix me
val revision = Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION)
fun Project.findVariantScope(variantName: String): VariantScope? {
    return findVariantManager().findVariantScope(variantName)
}


fun Project.findVariantManager(): VariantManager {
    return if (revision.major > 3 || revision.minor >= 6) {
        findVariantManager36()
    } else {
        findVariantManager35()
    }
}

fun Project.isDynamicFeature():Boolean{
    return if (revision.major > 3 || revision.minor >= 6) {
        isDynamicFeature36()
    } else {
        isDynamicFeature35()
    }
}

private fun Project.isDynamicFeature35():Boolean{
    return project.plugins.findPlugin(com.android.build.gradle.DynamicFeaturePlugin::class.java)!=null
}

private fun Project.isDynamicFeature36():Boolean{
    return project.plugins.findPlugin("com.android.internal.dynamic-feature")!=null
}

private fun Project.findVariantManager35(): VariantManager {
    val appPlugin = project.plugins.findPlugin(com.android.build.gradle.AppPlugin::class.java)
    return if(appPlugin == null){
        //DynamicFeaturePlugin
        val dynamicFeaturePlugin = project.plugins.findPlugin(com.android.build.gradle.DynamicFeaturePlugin::class.java)
        dynamicFeaturePlugin!!.variantManager
    }else{
        appPlugin.variantManager
    }
}

private fun Project.findVariantManager36(): VariantManager {
    val appPlugin = project.plugins.findPlugin("com.android.internal.application")
    return if(appPlugin == null){
        //DynamicFeaturePlugin
        val dynamicFeaturePlugin = project.plugins.findPlugin("com.android.internal.dynamic-feature")
        dynamicFeaturePlugin!!.let {
            it.javaClass.getMethod("getVariantManager").invoke(it) as VariantManager
        }
    }else{
        appPlugin.let {
            it.javaClass.getMethod("getVariantManager").invoke(it) as VariantManager
        }
    }
}

fun VariantManager.findVariantScope(variantName: String): VariantScope? {
    return if (revision.major < 4) {
        findVariantScope3X(variantName)
    } else if (revision.minor == 0) {
        findVariantScope40(variantName)
    } else {
        findVariantScope41(variantName)
    }
}

private fun VariantManager.findVariantScope3X(variantName: String): VariantScope? {
    return variantScopes.firstOrNull { it.fullVariantName == variantName }
}

private fun VariantManager.findVariantScope40(variantName: String): VariantScope? {
    return variantScopes.firstOrNull { it::class.java.getMethod("getName").invoke(it) == variantName }
}


private fun VariantManager.findVariantScope41(variantName: String): VariantScope? {
    for (info in this.javaClass.getMethod("getMainComponents").invoke(this) as List<Any>) {
        val properties = info.javaClass.getMethod("getProperties").invoke(info)
        if (properties.javaClass.getMethod("getName").invoke(properties) == variantName) {
            return properties.javaClass.getMethod("getVariantScope").invoke(properties) as VariantScope
        }
    }
    return null
}