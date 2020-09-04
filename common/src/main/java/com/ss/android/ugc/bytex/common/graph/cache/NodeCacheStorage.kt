package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.graph.Node
import java.io.File

/**
 * Created by yangzhiqian on 2020/9/3<br/>
 */
internal interface NodeCacheStorage :CacheStorage<File,Map<String,Node>>