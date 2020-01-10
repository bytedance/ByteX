package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.IGraphCache
import java.io.File

/**
 * Created by yangzhiqian on 2019-12-01<br/>
 */
object GraphCacheFactory {
    fun createFileGraphCacheHandler(): IGraphCache<File> {
        return GsonGraphCache
    }
}