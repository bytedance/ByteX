package com.ss.android.ugc.bytex.common.utils

import org.gradle.api.Action
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class FileHandler(val files: Collection<File>, val deepTraverse: Boolean = true) : Action<Consumer<FileHandler.FileData>> {
    private var consumer: Consumer<FileData>? = null
    override fun execute(consumer: Consumer<FileData>) {
        this.consumer = consumer
        val inputs = mutableListOf<PathResolver>()
        files.forEach { rootFile ->
            if (rootFile.isFile) {
                inputs.add(PathResolver(rootFile, "", deepTraverse))
            } else if (rootFile.isDirectory) {
                rootFile.traverseFile { singleFile ->
                    inputs.add(PathResolver(singleFile, singleFile.toRelativeString(rootFile), deepTraverse))
                }
            }
        }
        inputs.parallelStream().forEach {
            it.call()
        }
        this.consumer = null
    }

    private inner class PathResolver(val file: File, val toRelativeString: String, val deepTraverse: Boolean) : Callable<Unit> {
        override fun call() {
            if (file.name.let { it.endsWith("jar") || it.endsWith("zip") }) {
                try {
                    val dataList = LinkedList<FileData>()
                    file.unzip { name, data ->
                        val path = "$toRelativeString!$name"
                        if (deepTraverse) {
                            deepTraverse(path, data, Consumer {
                                dataList.add(it)
                            })
                        } else {
                            dataList.add(FileData(file, path, data))
                        }
                    }
                    dataList.parallelStream().forEach {
                        consumer!!.accept(it)
                    }
                    return
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            consumer!!.accept(FileData(file, toRelativeString, file.readBytes()))
        }

        private fun deepTraverse(path: String, bytes: ByteArray, simpleEntryConsumer: Consumer<FileData>) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
                var isZipFile = false
                var zipEntry: ZipEntry?
                while (zin.nextEntry.also { zipEntry = it } != null) {
                    isZipFile = true
                    if (!zipEntry!!.isDirectory) {
                        deepTraverse(path + "!" + zipEntry!!.name, zin.readBytes(), simpleEntryConsumer)
                    }
                }
                if (!isZipFile) {
                    simpleEntryConsumer.accept(FileData(file, path, bytes))
                }
            }
        }
    }

    class FileData(val file: File, val path: String, val bytes: ByteArray) {
        val name = path.lastIndexOf("!").let {
            if (it >= 0) {
                path.substring(it + 1)
            } else {
                path
            }
        }
    }
}
