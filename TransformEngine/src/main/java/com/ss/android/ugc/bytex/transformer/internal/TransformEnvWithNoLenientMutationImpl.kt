package com.ss.android.ugc.bytex.transformer.internal

import com.ss.android.ugc.bytex.gradletoolkit.Artifact
import com.ss.android.ugc.bytex.gradletoolkit.TransformEnv
import java.io.File


/**
 * Created by yangzhiqian on 2020-10-30.
 */
class TransformEnvWithNoLenientMutationImpl(val real: TransformEnv) : TransformEnv by real {
    val value: ThreadLocal<Boolean>? = try {
        val clazz = Class.forName("org.gradle.api.internal.project.DefaultProjectStateRegistry")
        val field = clazz.getDeclaredField("LENIENT_MUTATION_STATE")
        field.isAccessible = true
        field.get(clazz) as ThreadLocal<Boolean>
    } catch (e: ReflectiveOperationException) {
        e.printStackTrace()
        null
    }

    override fun getArtifact(artifact: Artifact): Collection<File> {
        return if (value != null) {
            val origin = value.get()
            try {
                value.set(true)
                real.getArtifact(artifact)
            } finally {
                value.set(origin)
            }
        } else {
            real.getArtifact(artifact)
        }
    }
}