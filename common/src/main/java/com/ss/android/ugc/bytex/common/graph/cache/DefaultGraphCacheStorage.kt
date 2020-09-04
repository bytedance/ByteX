package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Node
import java.io.File
import java.util.stream.Collectors

/**
 * Created by yangzhiqian on 2020/7/13<br/>
 */
internal class DefaultGraphCacheStorage(
        private val nodeCache: RamNodeCacheStorage?,
        private val classCache: ClassCacheStorage?) : GraphCacheStorage<File> {
    override fun loadCache(t: File?, graphBuilder: CachedGraphBuilder): Boolean {
        val nodes = nodeCache?.loadCache(t)
        if (nodes != null) {
            graphBuilder.addNodes(nodes)
            return true
        }
        val cache = classCache?.loadCache(t) ?: return false
        //经过抖音cache测试性能,结果多线程方案差不多，但大约结果是
        //Schedulers.COMPUTATION().submitAndAwait<parallelStream().forEach<forEach
        cache.parallelStream().forEach {
            graphBuilder.add(it, true)
        }
        return true
    }

    override fun saveCache(t: File?, nodes: Map<String, Node>): Boolean {
        var succeed = true
        if (nodeCache != null && !nodeCache.saveCache(t, nodes)) {
            succeed = false
        }
        if (classCache != null && !classCache.saveCache(t, nodes.values.stream().map { node -> node.entity }.collect(Collectors.toList()))) {
            succeed = false
        }
        return succeed
    }
}