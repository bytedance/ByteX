package com.ss.android.ugc.bytex.common.retrace

class InternalNameMappingProcessor(val real: MappingProcessor) : MappingProcessor {
    override fun processClassMapping(className: String, newClassName: String): Boolean {
        return real.processClassMapping(className.replace(".", "/"), newClassName.replace(".", "/"))
    }

    override fun processMethodMapping(className: String,
                                      firstLineNumber: Int,
                                      lastLineNumber: Int,
                                      methodReturnType: String,
                                      methodName: String,
                                      methodArguments: String,
                                      newClassName: String,
                                      newFirstLineNumber: Int,
                                      newLastLineNumber: Int,
                                      newMethodName: String) {
        real.processMethodMapping(
                className.replace(".", "/"),
                firstLineNumber,
                lastLineNumber,
                parseType(methodReturnType),
                methodName,
                methodArguments.split(",").map { parseType(it) }.joinToString(""),
                newClassName.replace(".", "/"),
                newFirstLineNumber,
                newLastLineNumber,
                newMethodName
        )
    }

    override fun processFieldMapping(className: String,
                                     fieldType: String,
                                     fieldName: String,
                                     newClassName: String,
                                     newFieldName: String) {
        real.processFieldMapping(
                className.replace(".", "/"),
                parseType(fieldType),
                fieldName,
                newClassName.replace(".", "/"),
                newFieldName
        )
    }

    private fun parseType(type: String): String {
        if (type.endsWith("[]")) {
            return "[${parseType(type.substring(0, type.length - 2))}"
        }
        return when (type) {
            "" -> {
                ""
            }
            "void" -> {
                "V"
            }
            "boolean" -> {
                "Z"
            }
            "byte" -> {
                "B"
            }
            "short" -> {
                "S"
            }
            "char" -> {
                "C"
            }
            "int" -> {
                "I"
            }
            "float" -> {
                "F"
            }
            "double" -> {
                "D"
            }
            "long" -> {
                "J"
            }
            else -> {
                "L${type.replace(".", "/")};"
            }
        }
    }
} 