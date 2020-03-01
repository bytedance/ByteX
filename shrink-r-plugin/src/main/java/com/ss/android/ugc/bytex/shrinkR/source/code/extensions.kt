package com.ss.android.ugc.bytex.shrinkR.source.code

import com.ss.android.ugc.bytex.shrinkR.source.AssignType
import com.ss.android.ugc.bytex.shrinkR.source.RFileWhiteList
import kotlin.streams.toList

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */

internal fun brewJava(lines: List<String>, className: String, limitSize: Int, verifyParse: Boolean, assignType: AssignType, whiteList: RFileWhiteList? = null): String {
    return RJavaCode().parse(lines).apply {
        if (verifyParse) {
            val parsedLines = toJavaFile().lines()
            lines.forEachIndexed { index, line ->
                //ignore blank
                if (line.replace(" ", "") != parsedLines[index].replace(" ", "")) {
                    throw RuntimeException("$index:[$line][${parsedLines[index]}]")
                }
            }
        }
        val publicClasses = codes.stream().filter { it is ClassTypeCode && it.access == "public" }.map { it as ClassTypeCode }.toList()
        if (publicClasses.size == 1) {
            publicClasses[0].className = className
            publicClasses[0].codes.filterIsInstance<ClassConstructorCode>().forEach {
                it.className = className
            }
        } else if (publicClasses.size > 1) {
            throw RuntimeException("too many public classes:[${publicClasses.joinToString(",", transform = { it.className })}]")
        }
        when (assignType) {
            AssignType.AssignValue -> adjustAssignValue(limitSize)
            AssignType.AssignInherit -> adjustAssignInherit(limitSize, whiteList)
        }
        verify()
    }.toJavaFile()
}


internal fun CodeContainer.adjustAssignValue(limitSize: Int) {
    codes.filterIsInstance<CodeContainer>().forEach { it.adjustAssignValue(limitSize) }
    if (this is ClassTypeCode) {
        val signedNotFinalFields = codes.stream()
                .filter { it is FieldTypeCode && it.isStatic && !it.isFinal && it.value != null && it.fieldType == "int" }
                .map { it as FieldTypeCode }
                .toList()
        if (signedNotFinalFields.size <= limitSize) {
            return
        }
        for (index in limitSize until signedNotFinalFields.size step limitSize) {
            val initFieldsMethod = StringBuilder()
            val methodName = "init\$${System.nanoTime()}"
            initFieldsMethod.appendCodeSpace(getDeep()).append("private static int $methodName () {")
            signedNotFinalFields.subList(index, minOf(signedNotFinalFields.size, index + limitSize)).forEach {
                initFieldsMethod.append(it.fieldName).append(" = ").append(it.value).append(";")
                it.value = null
            }
            initFieldsMethod.append("return 0;}")
            codes.add(codes.size - 1, FieldTypeCode(this, "private", true, true, "int", methodName, "$methodName()"))
            codes.add(codes.size - 1, UnDefinedCode(this, initFieldsMethod.toString()))
        }
    }
}

internal fun CodeContainer.adjustAssignInherit(limitSize: Int, whiteList: RFileWhiteList?) {
    codes.filterIsInstance<CodeContainer>().forEach { it.adjustAssignInherit(limitSize, whiteList) }
    if (this is ClassTypeCode) {
        var staticBlockInitSize = 0
        var nonStaticBlockInitSize = 0
        var constantPoolSize = 0
        val computeLimitSize = limitSize * 6
        val whiteListField = codes.filterIsInstance<FieldTypeCode>().filter {
            if (whiteList?.inWhiteList(it.classType.className, it.fieldName) == true) {
                if (it.isStatic) {
                    staticBlockInitSize += it.computeAssignInstructionSize()
                } else {
                    nonStaticBlockInitSize += it.computeAssignInstructionSize()
                }
                constantPoolSize += it.computeConstantPoolSize()
                true
            } else {
                false
            }
        }.toSet()
        val shouldOptimizeFields = codes.filterIsInstance<FieldTypeCode>().filter {
            if (whiteListField.contains(it)) {
                false
            } else {
                if (it.isStatic) {
                    staticBlockInitSize += it.computeAssignInstructionSize()
                } else {
                    nonStaticBlockInitSize += it.computeAssignInstructionSize()
                }
                constantPoolSize += it.computeConstantPoolSize()
                staticBlockInitSize >= computeLimitSize || nonStaticBlockInitSize >= computeLimitSize || constantPoolSize >= computeLimitSize
            }
        }
        if (shouldOptimizeFields.isEmpty()) {
            return
        }
        //config parent class

        val suffix = "_" + className.lastIndexOf("_").let {
            if (it < 0) {
                0
            } else {
                try {
                    Integer.parseInt(className.substring(it + 1)) + 1
                } catch (e: Exception) {
                    0
                }
            }
        }
        val realRType = className.substring(0, className.lastIndexOf("_").let {
            if (it < 0) {
                className.length
            } else {
                it
            }
        })
        val parentClassType = ClassTypeCode(container!!, null, isStatic, false, "$realRType$suffix")
        parentClassType.codes.add(ClassConstructorCode(parentClassType, "private", parentClassType.className, " () {}"))
        parentClassType.codes.addAll(shouldOptimizeFields)
        parentClassType.codes.add(ClassTypeEndCode(parentClassType))

        //remove field and config this class
        codes.removeAll(parentClassType.codes.filterIsInstance<FieldTypeCode>())
        parentClassName = parentClassType.className

        //config out container
        container!!.codes.add(container!!.codes.size - 1, parentClassType)
        parentClassType.adjustAssignInherit(limitSize, whiteList)
    }
}

internal fun StringBuilder.appendCodeSpace(deep: Int): StringBuilder {
    for (index in 0 until deep) {
        append("    ")
    }
    return this
}