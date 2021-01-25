package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.tasks.MergeSourceSetFolders
import java.io.File

internal fun MergeSourceSetFolders.assetSetList(): Collection<File> {
    return if (ANDROID_GRADLE_PLUGIN_VERSION.major >= 4 || ANDROID_GRADLE_PLUGIN_VERSION.minor >= 5) {
        assetSetList35_()
    } else {
        assetSetList30_()
    }
}


private fun MergeSourceSetFolders.assetSetList30_(): Collection<File> {
    //this.computeAssetSetList().flatMap { it.sourceFiles }.toSet()
    return ReflectionUtils.callMethod<List<Any>>(this, MergeSourceSetFolders::class.java, "computeAssetSetList", arrayOf(), arrayOf())
            .flatMap {
                ReflectionUtils.callPublicMethod<List<File>>(it, it.javaClass, "getSourceFiles", arrayOf(), arrayOf())
            }.toSet()
}

fun MergeSourceSetFolders.assetSetList35_(): Collection<File> {
    //this.computeAssetSetList$gradle().flatMap { it.sourceFiles }.toSet()
    return ReflectionUtils.callMethod<List<Any>>(this, MergeSourceSetFolders::class.java, "computeAssetSetList\$gradle", arrayOf(), arrayOf())
            .flatMap {
                ReflectionUtils.callPublicMethod<List<File>>(it, it.javaClass, "getSourceFiles", arrayOf(), arrayOf())
            }.toSet()
}
