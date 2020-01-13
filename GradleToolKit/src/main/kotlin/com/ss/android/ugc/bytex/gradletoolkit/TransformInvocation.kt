package com.ss.android.ugc.bytex.gradletoolkit

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.api.BaseVariant
import com.didiglobal.booster.gradle.variant

/**
 * Created by yangzhiqian on 2020-01-13<br/>
 */
fun getVariant(invocation: TransformInvocation): BaseVariant? {
    return invocation.variant
}