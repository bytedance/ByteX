package com.ss.android.ugc.bytex.common.graph.cache;

import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.graph.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CachedGraphBuilder(private val graphCacheFile: File?, shouldLoadCache: Boolean, private val shouldSaveCache: Boolean) : GraphBuilder() {
    private val graphCache =
            AsynchronousGraphCacheStorage(
                    DefaultGraphCacheStorage(
                            if (BooleanProperty.ENABLE_RAM_CACHE.value() && BooleanProperty.ENABLE_RAM_NODES_CACHE.value()) {
                                RamNodeCacheStorage
                            } else {
                                null
                            },
                            GsonFileClassCacheStorage(
                                    if (BooleanProperty.ENABLE_RAM_CACHE.value() && BooleanProperty.ENABLE_RAM_CLASSES_CACHE.value()) {
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
        //during incremental build, the classes which changed will be added again, and the calculation of the inheritance
        //relationship will be repeated. Replace it with a de-duplicated list here
        //增量时，原先的class changd会导致被add两次，计算继承关系会重复，这里替换成去重的list
        for (node in nodes.values) {
            if (node is ClassNode) {
                if (node.children.isNotEmpty() && node.children !is SkipDuplicatedList) {
                    node.children = SkipDuplicatedList(node.children)
                }
            } else if (node is InterfaceNode) {
                if (node.children.isNotEmpty() && node.children !is SkipDuplicatedList) {
                    node.children = SkipDuplicatedList(node.children)
                }
                if (node.implementedClasses.isNotEmpty() && node.implementedClasses !is SkipDuplicatedList) {
                    node.implementedClasses = SkipDuplicatedList(node.implementedClasses)
                }
            }
        }
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


    internal class SkipDuplicatedList<T>(c: Collection<T>) : LinkedList<T>(c) {

        override fun add(element: T): Boolean {
            if (contains(element)) {
                return false
            }
            return super.add(element)
        }

        override fun add(index: Int, element: T) {
            if (contains(element)) {
                return
            }
            super.add(index, element)
        }

        override fun addFirst(element: T) {
            if (contains(element)) {
                return
            }
            super.addFirst(element)
        }

        override fun addLast(element: T) {
            if (contains(element)) {
                return
            }
            super.addLast(element)
        }
    }
}
