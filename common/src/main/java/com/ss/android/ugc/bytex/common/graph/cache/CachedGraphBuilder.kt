package com.ss.android.ugc.bytex.common.graph.cache;

import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.graph.Node
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CachedGraphBuilder(private val graphCacheFile: File?, shouldLoadCache: Boolean, private val shouldSaveCache: Boolean) : GraphBuilder() {
    private var useRamCache = true

    constructor(graphCacheFile: File?, shouldLoadCache: Boolean, shouldSaveCache: Boolean, useRamCache: Boolean = true)
            : this(graphCacheFile, shouldLoadCache, shouldSaveCache) {
        this.useRamCache = useRamCache
    }

    private val graphCache =
            AsynchronousGraphCacheStorage(
                    DefaultGraphCacheStorage(
                            if (useRamCache && BooleanProperty.ENABLE_RAM_CACHE.value() && BooleanProperty.ENABLE_RAM_NODES_CACHE.value()) {
                                RamNodeCacheStorage
                            } else {
                                null
                            },
                            GsonFileClassCacheStorage(
                                    if (useRamCache && BooleanProperty.ENABLE_RAM_CACHE.value() && BooleanProperty.ENABLE_RAM_CLASSES_CACHE.value()) {
                                        RamClassesCacheStorage
                                    } else {
                                        null
                                    }
                            )
                    )
            )
    private val isCacheValid =
            try {
                shouldLoadCache && graphCache.loadCache(graphCacheFile, this).apply {
                    if (!this) {
                        RamClassesCacheStorage.clear()
                        RamNodeCacheStorage.clear()
                        graphCacheFile?.delete()
                        throw IllegalStateException("Failed to load cache")

                    }
                }
            } finally {
                if (graphCacheFile != null) {
                    graphCacheFile.delete()
                    RamClassesCacheStorage.clearCache(graphCacheFile)
                    RamNodeCacheStorage.clearCache(graphCacheFile)
                }
            }


    override fun isCacheValid(): Boolean = isCacheValid

    /**
     * from cache during incremental build
     */
    internal fun addNodes(nodes: Map<String, Node>) {
        if (nodeMap.isEmpty() && nodes is ConcurrentHashMap) {
            //减少复制
            nodeMap = nodes
        } else {
            nodeMap.putAll(nodes)
        }
    }

    @Synchronized
    override fun build(): Graph {
        val graph = super.build()
        if (shouldSaveCache) {
            if (BooleanProperty.ENABLE_ASYNC_SAVE_CACHE.value()) {
                graphCache.saveCacheAsync(graphCacheFile, nodeMap)
            } else {
                graphCache.saveCache(graphCacheFile, nodeMap)
            }
        }
        return graph
    }
}
