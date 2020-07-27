package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Graph
import java.io.File
import java.util.stream.Collectors

/**
 * Created by yangzhiqian on 2020/7/13<br/>
 */
class DefaultGraphCache(private val classCache: IClassCache) : IGraphCache<File> {
    override fun loadCache(t: File?, graphBuilder: CachedGraphBuilder): Boolean {
        val cache = classCache.loadCache(t) ?: return false
        //经过抖音cache测试性能,结果多线程方案差不多，但大约结果是
        //Schedulers.COMPUTATION().submitAndAwait<parallelStream().forEach<forEach
        cache.forEach {
            graphBuilder.add(it, true)
        }
        return true
    }

    override fun saveCache(t: File?, graph: Graph): Boolean {
        return classCache.saveCache(t, graph.nodes.values.stream().map { node -> node.entity }.collect(Collectors.toList()))
    }
}