package com.ss.android.ugc.bytex.common.graph.cache;

/**
 * Created by yangzhiqian on 2020-7-13<br/>
 */
interface ICache<T, D> {

    fun loadCache(t: T?): D?

    fun saveCache(t: T?, d: D): Boolean
}
