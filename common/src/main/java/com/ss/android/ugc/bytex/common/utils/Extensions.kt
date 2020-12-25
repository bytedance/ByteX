package com.ss.android.ugc.bytex.common.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */
fun <R> executeWithStartAndFinish(action: () -> R, start: () -> Unit, finish: (Exception?) -> Unit): R {
    try {
        start.invoke()
        val r = action.invoke()
        finish.invoke(null)
        return r
    } catch (e: Exception) {
        finish.invoke(e)
        throw e
    }
}

fun File.traverseFile(action: (File) -> Unit) {
    if (this.isFile) {
        action.invoke(this)
    }
    listFiles()?.forEach {
        it.traverseFile(action)
    }
}

fun File.unzip(action: (String, ByteArray) -> Unit) {
    ZipInputStream(BufferedInputStream(FileInputStream(this))).use { zin ->
        var zipEntry: ZipEntry? = zin.nextEntry
        while (zipEntry != null) {
            if (!zipEntry.isDirectory) {
                action.invoke(zipEntry.name, zin.readBytes())
            }
            zipEntry = zin.nextEntry
        }
    }
}