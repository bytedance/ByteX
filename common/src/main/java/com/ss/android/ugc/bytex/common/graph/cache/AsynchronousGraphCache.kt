package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import java.util.concurrent.Future

/**
 * Created by yangzhiqian on 2020/7/12<br/>
 */
class AsynchronousGraphCache<T>(realCache: IGraphCache<T>) : IGraphCache<T> by realCache {

    fun loadCacheAsync(t: T?, graphBuilder: CachedGraphBuilder): Future<Boolean> {
        return Schedulers.IO().submit { loadCache(t, graphBuilder) }
    }

    fun saveCacheAsync(t: T?, graph: Graph): Future<Boolean> {
        return Schedulers.IO().submit { saveCache(t, graph) }
    }
}