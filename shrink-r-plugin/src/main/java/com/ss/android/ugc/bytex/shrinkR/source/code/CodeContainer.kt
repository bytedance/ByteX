package com.ss.android.ugc.bytex.shrinkR.source.code

import java.util.*

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal abstract class CodeContainer(container: CodeContainer?) : Code(container) {
    val codes = LinkedList<Code>()
    fun getDeep(): Int {
        return if (container == null) {
            0
        } else {
            container!!.getDeep() + 1
        }
    }
}