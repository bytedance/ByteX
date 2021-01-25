package com.ss.android.ugc.bytex.common.utils

/**
 * Created by yangzhiqian on 2020/11/9<br/>
 */
class MethodMatcher(identity: String) {

    val ownerMatcher: StringMatcher
    val methodNameMatcher: StringMatcher
    val methodDescMatcher: StringMatcher

    init {
        val splits = if (identity.contains("#(")) {
            identity
        } else {
            identity.replaceFirst("(", "#(")
        }.split("#")
        if (splits.isEmpty() || splits.size > 3) {
            throw IllegalAccessException(identity)
        }
        ownerMatcher = StringMatcher.StringEqualMatcher(splits[0])
        methodNameMatcher = if (splits.size >= 2) {
            StringMatcher.StringEqualMatcher(splits[1])
        } else {
            StringMatcher.StringAllMatcher
        }
        methodDescMatcher = if (splits.size == 3) {
            StringMatcher.StringEqualMatcher(splits[2])
        } else {
            StringMatcher.StringAllMatcher
        }
    }

    fun match(owner: String, name: String, desc: String): Boolean {
        return ownerMatcher.match(owner) && methodNameMatcher.match(name) && methodDescMatcher.match(desc)
    }
}


sealed class StringMatcher(val identity: String) {
    abstract fun match(toMatch: String): Boolean

    object StringAllMatcher : StringMatcher("*") {
        override fun match(toMatch: String): Boolean = true
    }

    class StringEqualMatcher(identity: String) : StringMatcher(identity) {
        override fun match(toMatch: String): Boolean = identity == toMatch
    }
}

