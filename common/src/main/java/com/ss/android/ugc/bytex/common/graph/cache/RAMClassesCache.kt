package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.ClassEntity
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by yangzhiqian on 2020-03-04<br/>
 */
object RAMClassesCache : IClassCache {
    private val caches = ConcurrentHashMap<String, List<ClassEntity>>()

    fun clear() {
        caches.clear()
    }

    fun clearCache(f: File) {
        caches.remove(f.absolutePath)
    }

    override fun loadCache(t: File?): List<ClassEntity>? {
        if (t == null) {
            return null
        }
        return caches.remove(t.absolutePath)?.apply {
            println("Load ByteX Classes Cache Success[RAM]:" + t.absolutePath)
        }
    }

    override fun saveCache(t: File?, d: List<ClassEntity>): Boolean {
        if (t == null) {
            return false
        }
        println("Save ByteX Classes Cache Success[RAM]:" + t.absolutePath)
        caches[t.absolutePath] = d
        return true
    }
}