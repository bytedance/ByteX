package com.ss.android.ugc.bytex.common.retrace

import java.util.*

class FullMappingProcessor : ClassMappingProcessor() {
    // origin class name  -> member info set.
    val originClassFieldMap: MutableMap<String, MutableList<FieldInfo>> = HashMap()
    val originClassMethodMap: MutableMap<String, MutableList<MethodInfo>> = HashMap()

    // obfuscated class name  -> member info set.
    val obfuscatedClassFieldMap: MutableMap<String, MutableList<FieldInfo>> = HashMap()
    val obfuscatedClassMethodMap: MutableMap<String, MutableList<MethodInfo>> = HashMap()


    override fun processFieldMapping(className: String,
                                     fieldType: String,
                                     fieldName: String,
                                     newClassName: String,
                                     newFieldName: String) { // Obfuscated class name -> obfuscated field names.
        super.processFieldMapping(className, fieldType, fieldName, newClassName, newFieldName)
        val field = FieldInfo(fieldType, fieldName, newFieldName)
        originClassFieldMap.computeIfAbsent(className) { LinkedList() }.add(field)
        obfuscatedClassFieldMap.computeIfAbsent(origin2ObfuscatedlassMap.getOrDefault(className, className)) { LinkedList() }.add(field)
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
                                      newMethodName: String) { // Original class name -> obfuscated method names.
        super.processMethodMapping(className, firstLineNumber, lastLineNumber, methodReturnType, methodName, methodArguments, newClassName, newFirstLineNumber, newLastLineNumber, newMethodName)
        val method = MethodInfo(newFirstLineNumber,
                newLastLineNumber,
                methodReturnType,
                methodName,
                methodArguments,
                firstLineNumber,
                lastLineNumber,
                className,
                origin2ObfuscatedlassMap.getOrDefault(newClassName, newClassName),
                newMethodName)
        originClassMethodMap.computeIfAbsent(className) { LinkedList() }.add(method)
        obfuscatedClassMethodMap.computeIfAbsent(origin2ObfuscatedlassMap.getOrDefault(className, className)) { LinkedList() }.add(method)
    }

    class FieldInfo(val originalType: String,
                    val originalName: String,
                    val newFieldName: String)

    class MethodInfo(val obfuscatedFirstLineNumber: Int = 0,
                     val obfuscatedLastLineNumber: Int = 0,
                     val originalType: String,
                     val originalName: String,
                     val originalArguments: String,
                     val originalFirstLineNumber: Int = 0,
                     val originalLastLineNumber: Int = 0,
                     val originalClassName: String,
                     val newClassName: String,
                     val newMethodName: String)

}