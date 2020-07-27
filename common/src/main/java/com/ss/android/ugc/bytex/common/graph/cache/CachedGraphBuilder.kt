package com.ss.android.ugc.bytex.common.graph.cache;

import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import java.io.File
import java.util.concurrent.Future

class CachedGraphBuilder(private val graphCacheFile: File?, shouldLoadCache: Boolean, private val shouldSaveCache: Boolean) : GraphBuilder() {
    private val graphCache =
            AsynchronousGraphCache(
                    DefaultGraphCache(
                            GsonClassesCache(
                                    if (BooleanProperty.ENABLE_RAM_CACHE.value()) {
                                        RAMClassesCache
                                    } else {
                                        null
                                    }
                            )
                    )
            )
    private var grapCacheFuture: Future<Boolean>? = null
    private val isCacheValid =
            shouldLoadCache &&
                    if (BooleanProperty.ENABLE_ASYNC_LOAD_CACHE.value()) {
                        grapCacheFuture = graphCache.loadCacheAsync(graphCacheFile, this)
                        true
                    } else {
                        graphCache.loadCache(graphCacheFile, this).apply {
                            graphCacheFile?.delete()
                            if (graphCacheFile != null) {
                                RAMClassesCache.clearCache(graphCacheFile)
                            }
                            if (!this) {
                                throw IllegalStateException("Failed to load cache")
                            }
                        }
                    }

    override fun isCacheValid(): Boolean = isCacheValid

    @Synchronized
    override fun build(): Graph {
        //先保证cache执行完成
        grapCacheFuture?.get()?.apply {
            graphCacheFile?.delete()
            if (graphCacheFile != null) {
                RAMClassesCache.clearCache(graphCacheFile)
            }
            if (!this) {
                throw IllegalStateException("Failed to load cache")
            }
        }
        val graph = super.build()
        if (shouldSaveCache) {
            if (BooleanProperty.ENABLE_ASYNC_SAVE_CACHE.value()) {
                graphCache.saveCacheAsync(graphCacheFile, graph)
            } else {
                graphCache.saveCache(graphCacheFile, graph)
            }
        }
        return graph
    }
}
