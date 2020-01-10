package com.ss.android.ugc.bytex.gradletoolkit

/**
 * Created by tanlehua on 2019-04-30.
 */

import com.android.build.gradle.tasks.MergeResources
import java.io.File

fun MergeResources.resourceSetList(): List<File> {

    val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
            .firstOrNull { it.name == "computeResourceSetList" && it.parameterCount == 0 }
            ?: return emptyList()

    val oldIsAccessible = computeResourceSetListMethod.isAccessible
    try {
        computeResourceSetListMethod.isAccessible = true

        val resourceSets = computeResourceSetListMethod.invoke(this) as? Iterable<*>
                ?: return emptyList()

        return resourceSets.mapNotNull { resourceSet ->
            val getSourceFiles = resourceSet?.javaClass?.methods?.find { it.name == "getSourceFiles" && it.parameterCount == 0 }
            val files = getSourceFiles?.invoke(resourceSet)
            @Suppress("UNCHECKED_CAST")
            files as? Iterable<File>
        }.flatten()

    } finally {
        computeResourceSetListMethod.isAccessible = oldIsAccessible
    }
}