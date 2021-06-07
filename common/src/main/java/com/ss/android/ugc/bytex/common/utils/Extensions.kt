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

@Throws(IllegalArgumentException::class)
fun String.matchPair(left: String, right: String, startIndex: Int = 0, endIndex: Int = length): Pair<Int, Int>? {
    if (left == right) {
        throw IllegalArgumentException("left can not be equals right:${left}")
    }
    val sl = indexOf(left, startIndex)
    if (sl < 0) {
        return null
    }
    var start = sl + left.length
    var stack = 1
    while (stack != 0 && 0 < start && start < endIndex) {
        val startRight = indexOf(right, start)
        if (startRight < start) {
            //Not Closed
            throw IllegalArgumentException("Can matchPair by $left and $right from:$this")
        }
        val startLeft = indexOf(left, start)
        if (0 <= startLeft && startLeft < startRight) {
            //reach left
            stack++
            start = startLeft + left.length
        } else {
            //reach right
            stack--
            start = startRight + right.length
        }
    }
    if (stack != 0) {
        //Not Closed
        throw IllegalArgumentException("Can matchPair by $left and $right from:$this")
    }
    return Pair(sl, start)
}