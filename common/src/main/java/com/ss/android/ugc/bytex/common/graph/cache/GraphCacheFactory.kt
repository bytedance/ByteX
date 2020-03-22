package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.graph.IGraphCache
import java.io.File

/**
 * Created by yangzhiqian on 2019-12-01<br/>
 */
object GraphCacheFactory {
    fun createFileGraphCacheHandler(): IGraphCache<File> {
        return GsonGraphCache.apply {
            asyncSaveCache = BooleanProperty.ENABLE_ASYNC_CACHE.value()
            useRamCache(BooleanProperty.ENABLE_RAM_CACHE.value())
        }
    }
}