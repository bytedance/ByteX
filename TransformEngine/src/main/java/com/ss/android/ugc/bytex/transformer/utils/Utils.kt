package com.ss.android.ugc.bytex.transformer.utils

/**
 * Created by yangzhiqian on 2020/7/3<br/>
 * Desc:
 */
internal fun getStack(limit: Int, vararg skips: String): String {
    return Exception().stackTrace
            .map { it.toString() }
            .filter { stack ->
                !skips.any { stack.contains(it) }
            }
            .toList()
            .let {
                it.subList(0, Math.min(limit, it.size)).joinToString("\n")
            }
}