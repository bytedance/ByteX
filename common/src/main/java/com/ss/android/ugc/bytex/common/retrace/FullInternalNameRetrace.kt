package com.ss.android.ugc.bytex.common.retrace

import org.objectweb.asm.Type
import java.io.File

/**
 * retrace both origin2obfuscated and obfuscated2origin with internal name.
 * eg a/b/c/d/e/F->com/ss/android/ugc/bytex/App
 */
class FullInternalNameRetrace(mappingFile: File?) {
    private val mappingProcessor: FullMappingProcessor? = if (mappingFile == null || !mappingFile.exists()) {
        null
    } else {
        FullMappingProcessor().apply {
            MappingReader(mappingFile).pump(InternalNameMappingProcessor(this))
        }
    }

    fun originClassName(obfuscatedClass: String): String {
        if (null == mappingProcessor) {
            return obfuscatedClass
        }
        if (obfuscatedClass.startsWith("[")) {
            return "[" + originClassName(obfuscatedClass.substring(1))
        }
        return mappingProcessor.obfuscated2OriginClassMap[obfuscatedClass] ?: obfuscatedClass
    }

    private fun originClassDesc(type: Type): String {
        return when (type.sort) {
            Type.OBJECT -> {
                "L${originClassName(type.internalName)};"
            }
            Type.ARRAY -> {
                StringBuilder().let {
                    for (i in 0 until type.dimensions) {
                        it.append("[")
                    }
                    it.append(originClassDesc(type.elementType)).toString()
                }
            }
            else -> {
                type.descriptor
            }
        }
    }

    fun originMethod(obfuscatedClass: String, obfuscatedMethodName: String, obfuscatedMethodDesc: String): Triple<String, String, String> {
        if (null == mappingProcessor) {
            return Triple(obfuscatedClass, obfuscatedMethodName, obfuscatedMethodDesc)
        }
        val type = Type.getType(obfuscatedMethodDesc)
        val originReturnDesc = originClassDesc(type.returnType)
        val originArgs = type.argumentTypes.joinToString("") { originClassDesc(it) }
        val map = mappingProcessor.obfuscatedClassMethodMap[obfuscatedClass]?.firstOrNull {
            it.newMethodName == obfuscatedMethodName && it.originalType == originReturnDesc && it.originalArguments == originArgs
        }
        return if (map != null) {
            Triple(map.originalClassName, map.originalName, "(${map.originalArguments})${map.originalType}")
        } else if ("<clinit>" == obfuscatedMethodName) {
            Triple(originClassName(obfuscatedClass), obfuscatedMethodName, "(${originArgs})${originReturnDesc}")
        } else {
            throw IllegalStateException(Triple(originClassName(obfuscatedClass), obfuscatedMethodName, "(${originArgs})${originReturnDesc}").toString())
        }
    }

    fun originField(obfuscatedClass: String, obfuscatedFieldName: String, obfuscatedFieldDesc: String): Triple<String, String, String> {
        if (null == mappingProcessor) {
            return Triple(obfuscatedClass, obfuscatedFieldName, obfuscatedFieldDesc)
        }
        val typeDesc = originClassDesc(Type.getType(obfuscatedFieldDesc))
        val map = mappingProcessor.obfuscatedClassFieldMap[obfuscatedClass]?.firstOrNull {
            it.newFieldName == obfuscatedFieldName && it.originalType == typeDesc
        }
        return if (map != null) {
            Triple(originClassName(obfuscatedClass), map.originalName, map.originalType)
        } else {
            throw IllegalStateException(Triple(originClassName(obfuscatedClass), obfuscatedFieldName, typeDesc).toString())
        }
    }

    fun obfuscatedClassName(originClass: String): String {
        if (null == mappingProcessor) {
            return originClass
        }
        if (originClass.startsWith("[")) {
            return "[" + obfuscatedClassName(originClass.substring(1))
        }
        return mappingProcessor.origin2ObfuscatedlassMap[originClass] ?: originClass
    }

    private fun obfuscatedClassDesc(type: Type): String {
        return when (type.sort) {
            Type.OBJECT -> {
                "L${obfuscatedClassName(type.internalName)};"
            }
            Type.ARRAY -> {
                StringBuilder().let {
                    for (i in 0 until type.dimensions) {
                        it.append("[")
                    }
                    it.append(obfuscatedClassDesc(type.elementType)).toString()
                }
            }
            else -> {
                type.descriptor
            }
        }
    }

    fun obfuscatedMethod(originClass: String, originMethod: String, originDesc: String): Triple<String, String, String> {
        if (null == mappingProcessor) {
            return Triple(originClass, originMethod, originDesc)
        }
        val type = Type.getType(originDesc)
        val originReturnDesc = type.returnType.descriptor
        val originArgs = type.argumentTypes.joinToString("")
        val map = mappingProcessor.originClassMethodMap[originClass]?.firstOrNull {
            it.originalName == originMethod && it.originalArguments == originArgs && it.originalType == originReturnDesc
        }
        val obfuscatedReturnDesc = obfuscatedClassDesc(type.returnType)
        val obfuscatedArgsDesc = type.argumentTypes.joinToString("") { obfuscatedClassDesc(it) }
        return if (map != null) {
            Triple(obfuscatedClassName(originClass), map.newMethodName, "(${obfuscatedArgsDesc})${obfuscatedReturnDesc}")
        } else if ("<clinit>" == originMethod) {
            Triple(obfuscatedClassName(originClass), originMethod, "(${obfuscatedArgsDesc})${obfuscatedReturnDesc}")
        } else {
            throw IllegalStateException(Triple(obfuscatedClassName(originClass), originMethod, "(${obfuscatedArgsDesc})${obfuscatedReturnDesc}").toString())
        }
    }

    fun obfuscatedField(originClass: String, originField: String, originDesc: String): Triple<String, String, String> {
        if (null == mappingProcessor) {
            return Triple(originClass, originField, originDesc)
        }
        val map = mappingProcessor.originClassFieldMap[originClass]?.firstOrNull {
            it.originalName == originField && it.originalType == originDesc
        }
        return if (map != null) {
            Triple(obfuscatedClassName(originClass), map.newFieldName, obfuscatedClassDesc(Type.getType(originDesc)))
        } else {
            throw IllegalStateException(Triple(obfuscatedClassName(originClass), originField, obfuscatedClassDesc(Type.getType(originDesc))).toString())
        }
    }
}