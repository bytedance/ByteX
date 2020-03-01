package com.ss.android.ugc.bytex.shrinkR.source.code

import java.util.regex.Pattern

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal abstract class Code(var container: CodeContainer?) {
    abstract fun toStringCode(builder: StringBuilder)
    /**
     * @throws IllegalStateException
     */
    open fun verify() {}

    companion object {
        val ACCESS_TYPES = setOf("public", "protected", "private", "", null)

        val PATTERN_ClASS = Pattern.compile("\\s*((((public)|(protected)|(private)|(static)|(final))\\s+){0,3})class\\s+(\\w+)\\s*\\{\\s*")
        val PATTERN_ClASS_CONSTRUNCTOR = Pattern.compile("\\s*(((public)|(protected)|(private))\\s+)?(\\S+)(\\s*\\(.*}\\s*)")
        val PATTERN_FIELD = Pattern.compile("\\s*((((public)|(protected)|(private)|(static)|(final))\\s+){0,3})((int|int\\[])\\s+)(\\w+)\\s*(=\\s*(.+))?\\s*;\\s*")
        val PATTERN_CLASSEND = Pattern.compile("\\s*}\\s*")
        fun parseToCode(line: String, container: CodeContainer): Code {
            var code: Code? = parseToFieldTypeCode(line, container as? ClassTypeCode)
            if (code != null) {
                return code
            }
            code = parseToClassTypeCode(line, container)
            if (code != null) {
                return code
            }

            code = parseToClassConstructorCode(line, container as? ClassTypeCode)
            if (code != null) {
                return code
            }

            code = parseToClassTypeEndCode(line, container as? ClassTypeCode)
            if (code != null) {
                return code
            }

            return UnDefinedCode(container, line)
        }

        private fun parseToClassTypeCode(line: String, container: CodeContainer): ClassTypeCode? {
            val matcher = PATTERN_ClASS.matcher(line)
            if (matcher.matches()) {
                matcher.group(1).split(" ")
                        .filter { it.isNotBlank() }
                        .toList()
                        .also { list ->
                            list.toSet().also { set ->
                                //防止重复比如 public public public class问题
                                if (set.size != list.size) {
                                    return null
                                }
                            }
                        }
                val isPublic = matcher.group(4) != null
                val isProtected = matcher.group(5) != null
                val isPrivate = matcher.group(6) != null
                val isStatic = matcher.group(7) != null
                val isFinal = matcher.group(8) != null
                val className = matcher.group(9)!!
                return ClassTypeCode(
                        container,
                        if (isPublic && !isProtected && !isPrivate) {
                            "public"
                        } else if (!isPublic && isProtected && !isPrivate) {
                            "protected"
                        } else if (!isPublic && !isProtected && isPrivate) {
                            "private"
                        } else if (!isPublic && !isProtected && !isPrivate) {
                            null
                        } else {
                            return null
                        },
                        isStatic,
                        isFinal,
                        className)
            }
            return null
        }

        private fun parseToClassConstructorCode(line: String, outClass: ClassTypeCode?): ClassConstructorCode? {
            val matcher = PATTERN_ClASS_CONSTRUNCTOR.matcher(line)
            if (matcher.matches()) {
                return ClassConstructorCode(outClass!!, matcher.group(2), matcher.group(6), matcher.group(7))
            }
            return null
        }

        private fun parseToFieldTypeCode(line: String, outClass: ClassTypeCode?): FieldTypeCode? {
            val matcher = PATTERN_FIELD.matcher(line)
            if (matcher.matches()) {
                matcher.group(1).split(" ")
                        .filter { it.isNotBlank() }
                        .toList()
                        .also { list ->
                            list.toSet().also { set ->
                                //防止重复比如 public public public class问题
                                if (set.size != list.size) {
                                    return null
                                }
                            }
                        }
                val isPublic = matcher.group(4) != null
                val isProtected = matcher.group(5) != null
                val isPrivate = matcher.group(6) != null
                val isStatic = matcher.group(7) != null
                val isFinal = matcher.group(8) != null
                val fieldType = matcher.group(10)!!
                val fieldName = matcher.group(11)!!
                val fieldValue = matcher.group(13)
                return FieldTypeCode(
                        outClass!!,
                        if (isPublic && !isProtected && !isPrivate) {
                            "public"
                        } else if (!isPublic && isProtected && !isPrivate) {
                            "protected"
                        } else if (!isPublic && !isProtected && isPrivate) {
                            "private"
                        } else if (!isPublic && !isProtected && !isPrivate) {
                            null
                        } else {
                            return null
                        },
                        isStatic,
                        isFinal,
                        fieldType,
                        fieldName,
                        fieldValue)
            }
            return null
        }

        private fun parseToClassTypeEndCode(line: String, outClass: ClassTypeCode?): ClassTypeEndCode? {
            if (PATTERN_CLASSEND.matcher(line).matches()) {
                return ClassTypeEndCode(outClass!!)
            }
            return null
        }
    }
}