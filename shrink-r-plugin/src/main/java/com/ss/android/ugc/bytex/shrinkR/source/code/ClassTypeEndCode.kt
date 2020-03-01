package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal class ClassTypeEndCode(var outClass: ClassTypeCode) : Code(outClass) {
    override fun toStringCode(builder: StringBuilder) {
        builder.appendCodeSpace(outClass.getDeep()-1).append("}")
    }

    override fun verify() {
        super.verify()
        if (outClass.codes.indexOf(this) != outClass.codes.size - 1) {
            throw IllegalStateException()
        }
    }
}