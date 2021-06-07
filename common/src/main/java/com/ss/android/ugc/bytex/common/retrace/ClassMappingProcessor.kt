package com.ss.android.ugc.bytex.common.retrace

import java.util.*

open class ClassMappingProcessor : MappingProcessor {
    // Obfuscated class name -> original class name.
    val obfuscated2OriginClassMap: MutableMap<String, String> = HashMap()

    // original class name -> original Obfuscated name.
    val origin2ObfuscatedlassMap: MutableMap<String, String> = HashMap()

    // Implementations for MappingProcessor.
    override fun processClassMapping(className: String,
                                     newClassName: String): Boolean { // Obfuscated class name -> original class name.
        obfuscated2OriginClassMap[newClassName] = className
        origin2ObfuscatedlassMap[className] = newClassName
        return true
    }


    override fun processFieldMapping(className: String,
                                     fieldType: String,
                                     fieldName: String,
                                     newClassName: String,
                                     newFieldName: String) { // Obfuscated class name -> obfuscated field names.
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
    }
}