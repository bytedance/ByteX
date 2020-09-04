package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Node

/**
 * Created by yangzhiqian on 2020/7/13<br/>
 */

interface GraphCacheStorage<T> {
    fun loadCache(t: T?, graphBuilder: CachedGraphBuilder): Boolean
    fun saveCache(t: T?, nodes: Map<String, Node>): Boolean
}