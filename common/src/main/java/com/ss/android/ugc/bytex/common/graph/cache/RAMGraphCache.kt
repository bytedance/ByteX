package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.ClassEntity
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.graph.IGraphCache
import java.io.File
import java.util.stream.Collectors

/**
 * Created by yangzhiqian on 2020-03-04<br/>
 */
object RAMGraphCache : IGraphCache<File> {
    private val caches = HashMap<String, List<ClassEntity>>()

    fun clear() {
        caches.clear()
    }

    fun clearCache(f: File) {
        synchronized(caches) {
            caches.remove(f.absolutePath)
        }
    }

    override fun loadCache(t: File?, graphBuilder: GraphBuilder): Boolean {
        if (t == null) {
            return false
        }
        synchronized(caches) {
            caches.remove(t.absolutePath) ?: return false
        }.parallelStream().forEach {
            graphBuilder.add(it)
        }
        return true
    }

    override fun saveCache(t: File?, graph: Graph): Boolean {
        if (t == null) {
            return false
        }
        synchronized(caches) {
            caches.put(t.absolutePath, graph.nodes.values.stream().map { node -> node.entity }.collect(Collectors.toList<ClassEntity>()))
        }
        return true
    }
}