package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal class UnDefinedCode(container: CodeContainer, var code: String) : Code(container) {
    override fun toStringCode(builder: StringBuilder) {
        builder.append(code)
    }
}
