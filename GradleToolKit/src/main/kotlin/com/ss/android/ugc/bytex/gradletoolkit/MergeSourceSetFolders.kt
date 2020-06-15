package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.android.ide.common.resources.AssetSet
import java.io.File

/**
 * Created by tanlehua on 2019-04-30.
 */


fun MergeSourceSetFolders.assetSetList(): List<File> {
    val assetSets = try {
        assetSetList1()
    } catch (e: Exception) {
        assetSetList2()
    }
    return assetSets.flatMap { it.sourceFiles }.toSet().toList()
}


fun MergeSourceSetFolders.assetSetList1(): Iterable<AssetSet> {
    val computeAssetSetListMethod = MergeSourceSetFolders::class.java.declaredMethods
            .find { it.name == "computeAssetSetList" && it.parameterCount == 0 }!!

    val oldIsAccessible = computeAssetSetListMethod.isAccessible
    try {
        computeAssetSetListMethod.isAccessible = true
        return computeAssetSetListMethod.invoke(this) as Iterable<AssetSet>
    } finally {
        computeAssetSetListMethod.isAccessible = oldIsAccessible
    }
}

fun MergeSourceSetFolders.assetSetList2(): Iterable<AssetSet> {
    val computeAssetSetListMethod = MergeSourceSetFolders::class.java.declaredMethods
            .find { it.name == "computeAssetSetList\$gradle" && it.parameterCount == 0 }!!

    val oldIsAccessible = computeAssetSetListMethod.isAccessible
    try {
        computeAssetSetListMethod.isAccessible = true
        return computeAssetSetListMethod.invoke(this) as Iterable<AssetSet>
    } finally {
        computeAssetSetListMethod.isAccessible = oldIsAccessible
    }
}
