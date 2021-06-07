package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.internal.DependencyResourcesComputer
import com.android.build.gradle.tasks.MergeResources
import com.android.build.gradle.tasks.ResourceAwareTask
import com.android.ide.common.resources.ResourceSet
import org.gradle.api.provider.Property
import java.io.File


//todo fix reflection
internal fun MergeResources.resourceSetList(): Collection<File> {
    return if (ANDROID_GRADLE_PLUGIN_VERSION.major > 4 || (ANDROID_GRADLE_PLUGIN_VERSION.major == 4 && ANDROID_GRADLE_PLUGIN_VERSION.minor >= 1)) {
        resourceSetList41_()
    } else if (ANDROID_GRADLE_PLUGIN_VERSION.major >= 4 || ANDROID_GRADLE_PLUGIN_VERSION.minor >= 5) {
        resourceSetList35_()
    } else {
        resourceSetList30_()
    }
}

private fun MergeResources.resourceSetList30_(): Collection<File> {
    //this.computeResourceSetList().flatMap { it.sourceFiles }.toSet()
    return ReflectionUtils.callMethod<List<Any>>(this, MergeResources::class.java, "computeResourceSetList", arrayOf(), arrayOf())
            .flatMap {
                ReflectionUtils.callPublicMethod<List<File>>(it, it.javaClass, "getSourceFiles", arrayOf(), arrayOf())
            }.toSet()
}

private fun MergeResources.resourceSetList35_(): Collection<File> {
    //this.getConfiguredResourceSets(this.getPreprocessor()).flatMap { it.sourceFiles }.toSet()
    val preprocessor = ReflectionUtils.callMethod<Any>(this, MergeResources::class.java, "getPreprocessor", arrayOf(), arrayOf())
    return ReflectionUtils.callMethod<List<Any>>(this, MergeResources::class.java, "getConfiguredResourceSets", arrayOf(Class.forName("com.android.ide.common.resources.ResourcePreprocessor")), arrayOf(preprocessor))
            .flatMap {
                ReflectionUtils.callPublicMethod<List<File>>(it, it.javaClass, "getSourceFiles", arrayOf(), arrayOf())
            }.toSet()
}

private fun MergeResources.resourceSetList41_(): Collection<File> {
    //this.getConfiguredResourceSets(this.getPreprocessor(),this.getAaptEnv().getOrNull()).flatMap { it.sourceFiles }.toSet()
    val preprocessor = ReflectionUtils.callMethod<Any>(this, MergeResources::class.java, "getPreprocessor", arrayOf(), arrayOf())
    val aaptEnv = ReflectionUtils.callPublicMethod<Property<String>>(this, this::class.java, "getAaptEnv", arrayOf(), arrayOf()).orNull
    return ReflectionUtils.callMethod<List<Any>>(this, MergeResources::class.java, "getConfiguredResourceSets", arrayOf(Class.forName("com.android.ide.common.resources.ResourcePreprocessor"), String::class.java), arrayOf(preprocessor, aaptEnv))
        .flatMap {
            ReflectionUtils.callPublicMethod<List<File>>(it, it.javaClass, "getSourceFiles", arrayOf(), arrayOf())
        }.toSet()
}

fun MergeResources.dependenciesResourcesSet(): List<ResourceSet> {
    return if (ANDROID_GRADLE_PLUGIN_VERSION.major > 4 || (ANDROID_GRADLE_PLUGIN_VERSION.major == 4 && ANDROID_GRADLE_PLUGIN_VERSION.minor >= 1)) {
        dependenciesResourcesSet41_()
    } else {
        dependenciesResourcesSet35_()
    }
}

private fun MergeResources.dependenciesResourcesSet41_(): List<ResourceSet> {
    val computer = ReflectionUtils.callMethod<DependencyResourcesComputer>(this, ResourceAwareTask::class.java, "getResourcesComputer", arrayOf(), arrayOf());
    val aaptEnv = ReflectionUtils.callPublicMethod<Property<String>>(this, this::class.java, "getAaptEnv", arrayOf(), arrayOf()).orNull
    return ReflectionUtils.callMethod<List<ResourceSet>>(computer, DependencyResourcesComputer::class.java, "compute", arrayOf(Boolean::class.java, String::class.java), arrayOf(false, aaptEnv));
}

private fun MergeResources.dependenciesResourcesSet35_(): List<ResourceSet> {
    val computer = ReflectionUtils.callMethod<DependencyResourcesComputer>(this, ResourceAwareTask::class.java, "getResourcesComputer", arrayOf(), arrayOf());
    return ReflectionUtils.callMethod<List<ResourceSet>>(computer, DependencyResourcesComputer::class.java, "compute", arrayOf(Boolean::class.java), arrayOf(false));
}