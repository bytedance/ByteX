package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.tasks.MergeSourceSetFolders
import java.io.File

/**
 * Created by tanlehua on 2019-04-30.
 */


fun MergeSourceSetFolders.assetSetList(): List<File> {

    val computeAssetSetListMethod = MergeSourceSetFolders::class.java.declaredMethods
            .firstOrNull { it.name == "computeAssetSetList" && it.parameterCount == 0 }
            ?: return emptyList()

    val oldIsAccessible = computeAssetSetListMethod.isAccessible
    try {
        computeAssetSetListMethod.isAccessible = true

        val assetSets = computeAssetSetListMethod.invoke(this) as? Iterable<*>
                ?: return emptyList()

        return assetSets.mapNotNull { assetSet ->
            val getSourceFiles = assetSet?.javaClass?.methods?.find { it.name == "getSourceFiles" && it.parameterCount == 0 }
            val files = getSourceFiles?.invoke(assetSet)
            @Suppress("UNCHECKED_CAST")
            files as? Iterable<File>
        }.flatten()

    } finally {
        computeAssetSetListMethod.isAccessible = oldIsAccessible
    }

}