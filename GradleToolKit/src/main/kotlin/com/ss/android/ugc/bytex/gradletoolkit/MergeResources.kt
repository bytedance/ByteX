package com.ss.android.ugc.bytex.gradletoolkit

/**
 * Created by tanlehua on 2019-04-30.
 */

import com.android.build.gradle.tasks.MergeResources
import java.io.File

//todo fix reflection
fun MergeResources.resourceSetList(): List<File> {
    val resourceSets = try {
        resourceSetList1()
    } catch (e: Exception) {
        resourceSetList2()
    }
    return resourceSets.flatMap { it!!.javaClass.getMethod("getSourceFiles").invoke(it) as List<File> }.toSet().toList()
}


private fun MergeResources.resourceSetList1(): Iterable<*> {

    val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
            .find { it.name == "computeResourceSetList" && it.parameterCount == 0 }!!

    val oldIsAccessible = computeResourceSetListMethod.isAccessible
    try {
        computeResourceSetListMethod.isAccessible = true
        return computeResourceSetListMethod.invoke(this) as Iterable<*>
    } finally {
        computeResourceSetListMethod.isAccessible = oldIsAccessible
    }
}

private fun MergeResources.resourceSetList2(): Iterable<*> {
    val getConfiguredResourceSets = MergeResources::class.java.declaredMethods
            .find { it.name == "getConfiguredResourceSets" && it.parameterCount == 1 }!!

    val getPreprocessor = MergeResources::class.java.declaredMethods
            .find { it.name == "getPreprocessor" && it.parameterCount == 0 }!!

    val getConfiguredResourceSetsAccess = getConfiguredResourceSets.isAccessible
    val getPreprocessorAccess = getPreprocessor.isAccessible
    try {
        getConfiguredResourceSets.isAccessible = true
        getPreprocessor.isAccessible = true
        return getConfiguredResourceSets.invoke(this, getPreprocessor.invoke(this)) as Iterable<*>
    } finally {
        getConfiguredResourceSets.isAccessible = getConfiguredResourceSetsAccess
        getPreprocessor.isAccessible = getPreprocessorAccess
    }
}