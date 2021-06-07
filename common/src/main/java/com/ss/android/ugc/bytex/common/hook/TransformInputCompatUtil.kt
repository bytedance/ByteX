package com.ss.android.ugc.bytex.common.hook

import com.ss.android.ugc.bytex.common.utils.traverseFile
import org.objectweb.asm.ClassReader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.stream.Collectors

internal object TransformInputCompatUtil {
    private val DotClass = ".class"
    private val DotClassLength = DotClass.length
    fun covertToTransformInput(inputs: Collection<File>): Collection<File> {
        val regularFiles = LinkedList<File>()
        val classFiles = LinkedList<File>()
        inputs.forEach {
            it.traverseFile {
                if (it.name.endsWith(DotClass)) {
                    classFiles.add(it)
                } else {
                    regularFiles.add(it)
                }
            }
        }
        val classParentFiles = classFiles.parallelStream().map {
            val absolutePath = it.absolutePath
            absolutePath.substring(0, absolutePath.length - DotClassLength - readClassName(it).length - 1)
        }.collect(Collectors.toSet()).map { File(it) }
        return classParentFiles + regularFiles.filter { regularFile ->
            classParentFiles.none { regularFile.absolutePath.startsWith(it.absolutePath + "/") }
        }
    }

    private fun readClassName(classFile: File): String {
        return BufferedInputStream(FileInputStream(classFile)).use {
            ClassReader(it).className
        }
    }

}