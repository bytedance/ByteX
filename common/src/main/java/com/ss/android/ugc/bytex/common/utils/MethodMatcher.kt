package com.ss.android.ugc.bytex.common.utils

/**
 * Created by yangzhiqian on 2020/11/9<br/>
 */

fun main() {
    println(MethodMatcher("com/Class#abc").match("com/Class", "abc", "()V"))
    println(MethodMatcher("com/Class#abc").match("com/Class", "ab", "()V"))
    println(MethodMatcher("com/Class").match("com/Class", "ab", "()V"))
    println(MethodMatcher("com/Clas||com/Class").match("com/Class", "ab", "()V"))
    println(MethodMatcher("com/Clas&&com/Class").match("com/Class", "ab", "()V"))
    println(MethodMatcher("!com/Clas").match("com/Class", "ab", "()V"))
    println(MethodMatcher("(com/Clas||com/Class)&&!(com/Clas&&com/Class)").match("com/Class", "ab", "()V"))
    println(MethodMatcher("(com/Clas||com/Class||(com/Clas||com/Class)&&!(!com/Clas&&com/Class))&&!(!com/Clas&&com/Class)").match("com/Class", "ab", "()V"))
}

interface IMethodMatcher {
    fun match(owner: String, name: String, desc: String): Boolean
    fun classNameMathOnly(): Set<String>
}

open class MethodMatcher(val identity: String) : IMethodMatcher, Comparable<MethodMatcher> {
    private val methodMatchers by lazy {
        ArrayList<Any>().apply {
            MultiMethodMatcherIdentifyReader(identity).accept(object : MultiMethodMatcherIdentifyReader.MultiMethodMatcherIdentifyVisitor() {
                override fun visitIdentity(identity: String) {
                    add(SimpleMethodMatcher(identity))
                }

                override fun visitIdentityBlock(identity: String): MultiMethodMatcherIdentifyReader.MultiMethodMatcherIdentifyVisitor? {
                    add(MethodMatcher(identity))
                    return null
                }

                override fun visitOperation(operation: Operation) {
                    add(operation)
                }
            })
        }
    }

    override fun match(owner: String, name: String, desc: String): Boolean {
        var result = false
        var hasInv = false
        for (item in methodMatchers) {
            if (item is IMethodMatcher) {
                result = item.match(owner, name, desc)
                if (hasInv) {
                    result = !result
                }
            } else if (item is Operation) {
                when (item) {
                    Operation.AND -> {
                        if (!result) {
                            return false
                        }
                    }
                    Operation.OR -> {
                        if (result) {
                            return true
                        }
                    }
                    Operation.INV -> {
                        hasInv = !hasInv
                    }
                    else -> {
                        throw IllegalStateException(item.toString())
                    }
                }
            } else {
                throw IllegalStateException(item.toString())
            }
        }
        return result
    }

    override fun classNameMathOnly(): Set<String> = methodMatchers.filterIsInstance<IMethodMatcher>().flatMap { it.classNameMathOnly() }.toSet()
    override fun hashCode(): Int = identity.hashCode()
    override fun equals(other: Any?): Boolean = other is MethodMatcher && identity == other.identity
    override fun compareTo(other: MethodMatcher): Int = identity.compareTo(other.identity)

    private class MultiMethodMatcherIdentifyReader(val identity: String, val startIndex: Int = 0, val endIndex: Int = identity.length) {
        fun accept(visitor: MultiMethodMatcherIdentifyVisitor) {
            visitor.startBlock()
            var start = startIndex
            while (start < endIndex) {
                val matchPair = identity.matchPair(Operation.PAIR_LEFT.key, Operation.PAIR_RIGHT.key, start, endIndex)
                val simpleEnd = matchPair?.first ?: endIndex
                while (start < simpleEnd) {
                    val andIndex = identity.indexOf(Operation.AND.key, start)
                    val orIndex = identity.indexOf(Operation.OR.key, start)
                    val nivIndex = identity.indexOf(Operation.INV.key, start)
                    val end = listOf(andIndex, orIndex, nivIndex).filter { it >= 0 }.min() ?: -1
                    if (end < 0) {
                        visitor.visitIdentity(identity.substring(start, simpleEnd))
                        start = simpleEnd
                    } else {
                        if (start < end) {
                            visitor.visitIdentity(identity.substring(start, end))
                        }
                        start = end + if (end == andIndex) {
                            visitor.visitOperation(Operation.AND)
                            Operation.AND.key.length
                        } else if (end == orIndex) {
                            visitor.visitOperation(Operation.OR)
                            Operation.OR.key.length
                        } else {
                            visitor.visitOperation(Operation.INV)
                            Operation.INV.key.length
                        }
                    }
                }
                if (matchPair != null) {
                    val blockIdentity = identity.substring(matchPair.first + Operation.PAIR_LEFT.key.length, matchPair.second - Operation.PAIR_RIGHT.key.length)
                    visitor.visitIdentityBlock(blockIdentity)?.apply {
                        MultiMethodMatcherIdentifyReader(blockIdentity).accept(this)
                    }
                    start = matchPair.second
                } else {
                    break
                }
            }
            visitor.endBlock()
        }

        open class MultiMethodMatcherIdentifyVisitor {
            open fun startBlock() {}
            open fun visitIdentity(identity: String) {}

            open fun visitIdentityBlock(identity: String): MultiMethodMatcherIdentifyVisitor? {
                return null
            }

            open fun visitOperation(operation: Operation) {}
            open fun endBlock() {}
        }
    }

    enum class Operation(val key: String) {
        AND("&&"), OR("||"), INV("!"), PAIR_LEFT("{"), PAIR_RIGHT("}"), INHERIT("+");
    }
}

class InheritMethodMatcher(identity: String, childrenResolver: (String) -> Collection<String>) : MethodMatcher(
        identity.let {
            val replaceHandles = HashSet<String>()
            var start = 0
            while (start < it.length) {
                val inheritFlagIndex = it.indexOf(Operation.INHERIT.key, start)
                if (inheritFlagIndex >= 0) {
                    start = arrayOf(
                            //寻找带有)结尾的索引
                            try {
                                "${Operation.PAIR_LEFT.key}${it.substring(inheritFlagIndex)}".matchPair(Operation.PAIR_LEFT.key, Operation.PAIR_RIGHT.key)?.second?.let {
                                    it + inheritFlagIndex - Operation.PAIR_LEFT.key.length - Operation.PAIR_RIGHT.key.length
                                } ?: -1
                            } catch (e: IllegalArgumentException) {
                                //不是以)结尾
                                -1
                            },
                            it.indexOf(Operation.AND.key, inheritFlagIndex),
                            it.indexOf(Operation.OR.key, inheritFlagIndex)
                    ).filter {
                        it >= 0
                    }.min() ?: it.length
                    replaceHandles.add(it.substring(inheritFlagIndex, start))
                } else {
                    break
                }
            }
            var result = it
            replaceHandles.forEach {
                val classMethodSpilterIndex = it.indexOf("#")
                val className = if (classMethodSpilterIndex < 0) {
                    it.substring(Operation.INHERIT.key.length)
                } else {
                    it.substring(Operation.INHERIT.key.length, classMethodSpilterIndex)
                }
                val method = if (classMethodSpilterIndex < 0) {
                    ""
                } else {
                    it.substring(classMethodSpilterIndex)
                }
                val resolvedMethods = childrenResolver.invoke(className).map { "${it}${method}" }.toList()
                val resolvedIdentify = if (resolvedMethods.isNotEmpty()) {
                    "${Operation.PAIR_LEFT.key}${it.substring(Operation.INHERIT.key.length)}${Operation.OR.key}${resolvedMethods.joinToString(Operation.OR.key)}${Operation.PAIR_RIGHT.key}"
                } else {
                    it.substring(Operation.INHERIT.key.length)
                }
                result = result.replace(it, resolvedIdentify)
            }
            result
        }
)

class SimpleMethodMatcher(private val identity: String) : IMethodMatcher, Comparable<SimpleMethodMatcher> {
    private val classNameMathOnly: String
    private val ownerMatcher: StringMatcher
    private val methodNameMatcher: StringMatcher
    private val methodDescMatcher: StringMatcher

    init {
        val splits = if (identity.contains("#(")) {
            identity
        } else {
            identity.replaceFirst("(", "#(")
        }.split("#")
        if (splits.isEmpty() || splits.size > 3) {
            throw IllegalAccessException(identity)
        }
        classNameMathOnly = splits[0]
        ownerMatcher = StringMatcher.StringEqualMatcher(classNameMathOnly)
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

    override fun match(owner: String, name: String, desc: String): Boolean {
        return ownerMatcher.match(owner) && methodNameMatcher.match(name) && methodDescMatcher.match(desc)
    }

    override fun classNameMathOnly(): Set<String> = setOf(classNameMathOnly)
    override fun hashCode(): Int = identity.hashCode()
    override fun equals(other: Any?): Boolean = other is SimpleMethodMatcher && identity == other.identity
    override fun compareTo(other: SimpleMethodMatcher): Int = identity.compareTo(other.identity)
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

