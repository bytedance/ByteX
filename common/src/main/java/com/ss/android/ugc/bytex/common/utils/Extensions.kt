package com.ss.android.ugc.bytex.common.utils

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */
fun <R> executeWithStartAndFinish(action: () -> R, start: () -> Unit, finish: (Exception?) -> Unit): R {
    try {
        start.invoke()
        val r = action.invoke()
        finish.invoke(null)
        return r
    } catch (e: Exception) {
        finish.invoke(e)
        throw e
    }
}