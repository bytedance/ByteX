package com.ss.android.ugc.bytex.gradletoolkit

/**
 * Created by tanlehua on 2019-04-30.
 */

import com.android.build.gradle.tasks.MergeResources
import com.android.ide.common.resources.ResourceSet
import java.io.File

fun MergeResources.resourceSetList(): List<File> {
    val resourceSets = try {
        resourceSetList1()
    } catch (e: Exception) {
        resourceSetList2()
    }
    return resourceSets.flatMap { it.sourceFiles }.toSet().toList()
}


fun MergeResources.resourceSetList1(): Iterable<ResourceSet> {

    val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
            .find { it.name == "computeResourceSetList" && it.parameterCount == 0 }!!

    val oldIsAccessible = computeResourceSetListMethod.isAccessible
    try {
        computeResourceSetListMethod.isAccessible = true
        return computeResourceSetListMethod.invoke(this) as Iterable<ResourceSet>
    } finally {
        computeResourceSetListMethod.isAccessible = oldIsAccessible
    }
}

fun MergeResources.resourceSetList2(): Iterable<ResourceSet> {
    val getConfiguredResourceSets = MergeResources::class.java.declaredMethods
            .find { it.name == "getConfiguredResourceSets" && it.parameterCount == 1 }!!

    val getPreprocessor = MergeResources::class.java.declaredMethods
            .find { it.name == "getPreprocessor" && it.parameterCount == 0 }!!

    val getConfiguredResourceSetsAccess = getConfiguredResourceSets.isAccessible
    val getPreprocessorAccess = getPreprocessor.isAccessible
    try {
        getConfiguredResourceSets.isAccessible = true
        getPreprocessor.isAccessible = true
        return getConfiguredResourceSets.invoke(this, getPreprocessor.invoke(this)) as Iterable<ResourceSet>
    } finally {
        getConfiguredResourceSets.isAccessible = getConfiguredResourceSetsAccess
        getPreprocessor.isAccessible = getPreprocessorAccess
    }
}