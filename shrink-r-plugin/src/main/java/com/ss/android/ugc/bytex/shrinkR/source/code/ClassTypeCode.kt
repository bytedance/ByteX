package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal class ClassTypeCode(container: CodeContainer, var access: String?, var isStatic: Boolean, var isFinal: Boolean, var className: String) : CodeContainer(container) {
    var parentClassName: String? = null
    override fun toStringCode(builder: StringBuilder) {
        builder.appendCodeSpace(getDeep() - 1)
        if (!access.isNullOrBlank()) {
            builder.append(access).append(" ")
        }
        if (isStatic) {
            builder.append("static ")
        }
        if (isFinal) {
            builder.append("final ")
        }
        builder.append("class ").append(className)
        if (!parentClassName.isNullOrBlank()) {
            builder.append(" extends ").append(parentClassName)
        }
        builder.append(" {")
        codes.forEach {
            builder.append("\n")
            it.toStringCode(builder)
        }
    }

    override fun verify() {
        super.verify()
        if (codes.isEmpty()) {
            throw IllegalStateException()
        }
        codes.forEachIndexed { index, code ->
            code.verify()
        }
        if (codes.last !is ClassTypeEndCode) {
            throw IllegalStateException()
        }
    }
}