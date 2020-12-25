package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.Task
import java.lang.reflect.Field
import java.util.*

/**
 * Created by yangzhiqian on 2020-01-13<br/>
 */
val TransformInvocation.project: Project
    get() = context.let { context ->
        if (context is Task) {
            context.project
        } else {
            try {
                (context.javaClass.getDeclaredField("this$1").apply {
                    isAccessible = true
                }.get(context)?.run {
                    javaClass.getDeclaredField("this$0").apply {
                        isAccessible = true
                    }.get(this) as? Task
                })?.project
            } catch (e: ReflectiveOperationException) {
                null
            } ?: context.javaClass.allFields().filter { it.type == Project::class.java }.map {
                it.isAccessible = true
                it.get(context)
            }.filterIsInstance<Project>().firstOrNull()!!
        }
    }

private fun Class<*>.allFields(): List<Field> {
    val result = LinkedList<Field>()
    result.addAll(declaredFields)
    superclass?.allFields()?.apply {
        result.addAll(this)
    }
    return result
}

val TransformInvocation.variant: BaseVariant
    get() = project.extensions.getByName("android").let { android ->
        this.context.variantName.let { variant ->
            when (android) {
                is AppExtension -> when {
                    variant.endsWith("AndroidTest") -> android.testVariants.single { it.name == variant }
                    variant.endsWith("UnitTest") -> android.unitTestVariants.single { it.name == variant }
                    else -> android.applicationVariants.single { it.name == variant }
                }
                is LibraryExtension -> android.libraryVariants.single { it.name == variant }
                else -> TODO("variant not found")
            }
        }
    }
