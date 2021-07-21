package com.ss.android.ugc.bytex.common.graph.cache

import com.ss.android.ugc.bytex.common.configuration.BooleanProperty
import com.ss.android.ugc.bytex.common.graph.ClassNode
import com.ss.android.ugc.bytex.common.graph.InterfaceNode
import com.ss.android.ugc.bytex.common.graph.Node
import com.ss.android.ugc.bytex.common.utils.GradleDaemonIgnoreClassLoaderSingletonManager
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by yangzhiqian on 2020/9/3<br/>
 */
internal class RamNodeCacheStorage : NodeCacheStorage {
    private val caches =
            if (BooleanProperty.ENABLE_GRADLE_DAEMON_IGNORE_CLASSLOADER_SINGLETON.value()) {
                GradleDaemonIgnoreClassLoaderSingletonManager.computeIfAbsent<ConcurrentHashMap<String, Map<String, Node>>>(this, this.javaClass.name) {
                    ConcurrentHashMap()
                }
            } else {
                ConcurrentHashMap()
            }

    fun clear() {
        caches.clear()
    }

    fun clearCache(f: File) {
        caches.remove(f.absolutePath)
    }


    override fun loadCache(t: File?): Map<String, Node>? {
        if (t == null) {
            return null
        }
        return caches.remove(t.absolutePath)?.apply {
            println("Load ByteX Nodes Cache(${size}) Success[RAM]:" + t.absolutePath)
        }
    }

    override fun saveCache(t: File?, d: Map<String, Node>): Boolean {
        if (t == null) {
            return false
        }
        //during incremental build, the classes which changed will be added again, and the calculation of the inheritance
        //relationship will be repeated. Replace it with a de-duplicated list here
        //增量时，原先的class changd会导致被add两次，计算继承关系会重复，这里替换成去重的list
        for (node in d.values) {
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
        caches[t.absolutePath] = d
        println("Save ByteX Nodes Cache(${d.size}) Success[RAM]:" + t.absolutePath)
        return true
    }

    internal class SkipDuplicatedList<T>(c: Collection<T>) : LinkedList<T>(c) {
        override fun add(element: T): Boolean {
            if (contains(element)) {
                return false
            }
            return super.add(element)
        }
    }
}