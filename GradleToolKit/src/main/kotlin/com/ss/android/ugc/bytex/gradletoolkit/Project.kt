@file:JvmName("Project_")

package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.repository.Revision
import org.gradle.api.Project
import java.util.*

/**
 * Returns android extension
 *
 * @author johnsonlee
 */
inline fun <reified T : BaseExtension> Project.getAndroid(): T {
    return extensions.getByName("android") as T
}

/**
 * The gradle version
 */
val Project.gradleVersion: Revision
    get() {
        return Revision.parseRevision(gradle.gradleVersion)
    }

fun Project.assembleVariantProcessor() {
    when {
        project.plugins.hasPlugin("com.android.application") -> project.getAndroid<AppExtension>().let { android ->
            project.afterEvaluate {
                ServiceLoader.load(VariantProcessor::class.java, javaClass.classLoader).toList().let { processors ->
                    android.applicationVariants.forEach { variant ->
                        processors.forEach { processor ->
                            processor.process(variant)
                        }
                    }
                }
            }
        }
        project.plugins.hasPlugin("com.android.library") -> project.getAndroid<LibraryExtension>().let { android ->
            project.afterEvaluate {
                ServiceLoader.load(VariantProcessor::class.java, javaClass.classLoader).toList().let { processors ->
                    android.libraryVariants.forEach { variant ->
                        processors.forEach { processor ->
                            processor.process(variant)
                        }
                    }
                }
            }
        }
    }
}