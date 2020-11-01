package com.ss.android.ugc.bytex.transformer.internal

import com.ss.android.ugc.bytex.gradletoolkit.Artifact
import com.ss.android.ugc.bytex.gradletoolkit.TransformEnv
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.execution.timeout.Timeout
import org.gradle.internal.execution.timeout.TimeoutHandler
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeoutException


/**
 * Created by yangzhiqian on 2020-10-20.
 */
@Deprecated("use TransformEnvWithNoLenientMutationImpl")
class TransformEnvWithTimeoutImpl(project: Project, val timeout: Duration, val real: TransformEnv) : TransformEnv by real {
    private val timeoutHandler: TimeoutHandler by lazy {
        (project as ProjectInternal).services.get(TimeoutHandler::class.java)
    }

    override fun getArtifact(artifact: Artifact): Collection<File> {
        val taskTimeout: Timeout = timeoutHandler.start(Thread.currentThread(), timeout)
        try {
            return real.getArtifact(artifact)
        } finally {
            if (taskTimeout.stop()) {
                Thread.interrupted()
                TimeoutException("Timeout has been exceeded").printStackTrace()
                return emptyList()
            }
        }
    }
}