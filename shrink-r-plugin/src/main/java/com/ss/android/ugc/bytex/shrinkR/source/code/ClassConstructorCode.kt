package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal class ClassConstructorCode(var outClass: ClassTypeCode, var access: String?, var className: String, var content: String) : Code(outClass) {

    override fun toStringCode(builder: StringBuilder) {
        builder.appendCodeSpace(outClass.getDeep())
        if (!access.isNullOrBlank()) {
            builder.append(access).append(" ")
        }
        builder.append(className).append(content)
    }

    override fun verify() {
        super.verify()
        if (outClass.className != className || !ACCESS_TYPES.contains(access)) {
            throw IllegalStateException()
        }
    }
}
