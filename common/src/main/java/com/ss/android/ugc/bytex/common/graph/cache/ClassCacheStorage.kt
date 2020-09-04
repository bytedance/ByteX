package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.ClassEntity
import java.io.File

/**
 * Created by yangzhiqian on 2020/7/13<br/>
 */
internal interface ClassCacheStorage : CacheStorage<File, List<ClassEntity>>