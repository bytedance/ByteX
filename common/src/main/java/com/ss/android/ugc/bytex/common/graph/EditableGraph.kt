package com.ss.android.ugc.bytex.common.graph

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by yangzhiqian on 2020/9/3<br/>
 */
internal class EditableGraph(map: Map<String, Node>) : Graph(map) {
    /**
     * clear graph info
     * called by internal,please do not call.
     */
    fun clear() {
        nodeMap = ConcurrentHashMap()
    }
}