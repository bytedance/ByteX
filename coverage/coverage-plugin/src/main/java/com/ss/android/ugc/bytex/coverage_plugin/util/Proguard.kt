package com.ss.android.ugc.bytex.coverage_plugin.util

import java.io.File

/**
 * Created by jiangzilai on 2019-10-14.
 */

fun initProguardMapping(mappingFile: File): HashMap<String, String> {
    val map = HashMap<String, String>(70000)
    mappingFile.readLines().forEach {
        if (it.trim().endsWith(":")) {
            val split = it.substring(0, it.length - 1).split("->")
            val raw = split[0].trim().replace(".", "/")
            val proguard = split[1].trim().replace(".", "/")
            map[proguard] = raw
        }
    }
    return map
}