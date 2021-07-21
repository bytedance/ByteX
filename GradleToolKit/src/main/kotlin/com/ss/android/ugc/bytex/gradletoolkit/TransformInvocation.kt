package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.api.BaseVariant
import com.bytedance.gradle.compat.extension.project
import com.bytedance.gradle.compat.extension.variant
import org.gradle.api.Project

/**
 * Created by yangzhiqian on 2020-01-13<br/>
 */
val TransformInvocation.project: Project
    get() = project


val TransformInvocation.variant: BaseVariant
    get() = variant.raw()
