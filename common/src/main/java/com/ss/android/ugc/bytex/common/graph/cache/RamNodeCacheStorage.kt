package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Node
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by yangzhiqian on 2020/9/3<br/>
 */
internal object RamNodeCacheStorage : NodeCacheStorage {
    private val caches = ConcurrentHashMap<String, Map<String, Node>>()

    fun clear() {
        caches.clear()
    }

    fun clearCache(f: File) {
        caches.remove(f.absolutePath)
    }


    override fun loadCache(t: File?): Map<String, Node>? {
        if (t == null) {
            return null
        }
        return caches.remove(t.absolutePath)?.apply {
            println("Load ByteX Nodes Cache(${size}) Success[RAM]:" + t.absolutePath)
        }
    }

    override fun saveCache(t: File?, d: Map<String, Node>): Boolean {
        if (t == null) {
            return false
        }
        println("Save ByteX Nodes Cache(${d.size}) Success[RAM]:" + t.absolutePath)
        caches[t.absolutePath] = d
        return true
    }
}