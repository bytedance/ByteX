package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-12<br/>
 * Desc:
 */
internal class RJavaCode : CodeContainer(null) {
    fun parse(lines: List<String>): RJavaCode {
        var currentCodeContainer: CodeContainer = this
        lines.forEach {
            val code = parseToCode(it, currentCodeContainer)
            currentCodeContainer.codes.add(code)
            if (code is ClassTypeCode) {
                currentCodeContainer = code
            }
            if (code is ClassTypeEndCode) {
                currentCodeContainer = currentCodeContainer.container!!
            }
        }
        return this
    }

    override fun toStringCode(builder: StringBuilder) {
        codes.forEach {
            it.toStringCode(builder)
            builder.append("\n")
        }
    }

    override fun verify() {
        codes.forEach { it.verify() }
    }

    fun toJavaFile(): String {
        val result = StringBuilder()
        toStringCode(result)
        return result.toString()
    }
}



