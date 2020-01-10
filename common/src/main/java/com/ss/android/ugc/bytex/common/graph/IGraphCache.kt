package com.ss.android.ugc.bytex.common.graph;

/**
 * Created by yangzhiqian on 2019-12-01<br/>
 */
interface IGraphCache<T> {

    fun loadCache(t: T?, graphBuilder: GraphBuilder): Boolean

    fun saveCache(t: T?, graph: Graph): Boolean
}
