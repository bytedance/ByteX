package com.ss.android.ugc.bytex.shrinkR.source.code

/**
 * Created by yangzhiqian on 2020-02-14<br/>
 * Desc:
 */
internal class FieldTypeCode(var classType: ClassTypeCode, var access: String?, var isStatic: Boolean, var isFinal: Boolean, var fieldType: String, var fieldName: String, var value: String?) : Code(classType) {
    override fun toStringCode(builder: StringBuilder) {
        builder.appendCodeSpace(classType.getDeep())
        if (!access.isNullOrBlank()) {
            builder.append(access).append(" ")
        }
        if (isStatic) {
            builder.append("static ")
        }
        if (isFinal) {
            builder.append("final ")
        }
        builder.append(fieldType).append(" ")
        builder.append(fieldName)
        if (value != null) {
            builder.append(" = ").append(value)
        }
        builder.append(";")
    }

    fun computeConstantPoolSize(): Int {
        val value = this.value
        val intSize = if (value == null) {
            0
        } else {
            if (fieldType == "int") {
                1
            } else {
                if (value.replace(" ", "") == "{}") {
                    2
                } else {
                    var startIndex = 0
                    var size = 3
                    while (startIndex < value.length) {
                        startIndex = value.indexOf(",", startIndex)
                        if (startIndex < 0) {
                            break
                        }
                        size++
                        startIndex++
                    }
                    size
                }
            }
        }
        return if (intSize == 0) {
            1
        } else if (intSize == 1) {
            if (isStatic && isFinal) {
                2
            } else {
                4
            }
        } else if (intSize == 2) {
            3
        } else {
            intSize + 1
        }
    }

    fun computeAssignInstructionSize(): Int {
        if (value == null || ("int" == fieldType && isStatic && isFinal)) {
            //没有赋值或者是常量
            return 0
        }
        //int 赋值
        if ("int" == fieldType) {
            return 6
        }
        //int数组赋值
        if (value!!.replace(" ", "") == "{}") {
            return 6
        } else {
            var startIndex = 0
            var size = 1
            while (startIndex < value!!.length) {
                startIndex = value!!.indexOf(",", startIndex)
                if (startIndex < 0) {
                    break
                }
                size++
                startIndex++
            }
            //<6时使用ICONST_x指令
            return size * 11 - if (size <= 6) {
                size
            } else {
                6
            }
        }
    }

    override fun verify() {
        super.verify()
        if (!ACCESS_TYPES.contains(access) || !("int" == fieldType || "int[]" == fieldType)) {
            throw IllegalStateException()
        }
    }
}