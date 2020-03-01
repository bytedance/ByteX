package com.ss.android.ugc.bytex.shrinkR.source

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
enum class AssignType(val type: String) {
    AssignValue("value"),
    AssignInherit("inherit");
}

fun parseFormString(str: String): AssignType {
    str.trim().toLowerCase().apply {
        if (this == AssignType.AssignValue.type) {
            return AssignType.AssignValue
        } else if (this == AssignType.AssignInherit.type) {
            return AssignType.AssignInherit
        } else {
            throw IllegalArgumentException("can not parse [$str] to AssignType,[value,inherit] only")
        }
    }
}