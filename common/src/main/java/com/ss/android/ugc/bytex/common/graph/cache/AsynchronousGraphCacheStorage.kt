package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Node
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import java.util.concurrent.Future

/**
 * Created by yangzhiqian on 2020/7/12<br/>
 */
internal class AsynchronousGraphCacheStorage<T>(realCacheStorage: GraphCacheStorage<T>) : GraphCacheStorage<T> by realCacheStorage {

    fun loadCacheAsync(t: T?, graphBuilder: CachedGraphBuilder): Future<Boolean> {
        return Schedulers.IO().submit { loadCache(t, graphBuilder) }
    }

    fun saveCacheAsync(t: T?, nodes: Map<String, Node>): Future<Boolean> {
        return Schedulers.IO().submit { saveCache(t, nodes) }
    }
}