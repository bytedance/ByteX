package com.ss.android.ugc.bytex.common.graph.cache;

import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import java.io.File

class CachedGraphBuilder(private val graphCacheFile: File?, shouldLoadCache: Boolean, private val shouldSaveCache: Boolean) : GraphBuilder() {
    private val fileGraphCacheHandler = GraphCacheFactory.createFileGraphCacheHandler()
    private val isCacheValid = shouldLoadCache && fileGraphCacheHandler.loadCache(graphCacheFile, this)

    init {
        graphCacheFile?.delete()
        if (graphCacheFile != null) {
            RAMGraphCache.clearCache(graphCacheFile)
        }
    }

    override fun isCacheValid(): Boolean = isCacheValid

    override fun build(): Graph {
        val graph = super.build()
        if (shouldSaveCache) {
            fileGraphCacheHandler.saveCache(graphCacheFile, graph)
        }
        return graph
    }
}
