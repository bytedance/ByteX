package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Graph
import java.util.concurrent.Future

/**
 * Created by yangzhiqian on 2020/7/13<br/>
 */

interface IGraphCache<T> {
    fun loadCache(t: T?, graphBuilder: CachedGraphBuilder): Boolean
    fun saveCache(t: T?, graph: Graph): Boolean
}