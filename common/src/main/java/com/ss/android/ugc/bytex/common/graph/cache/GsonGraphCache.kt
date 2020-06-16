package com.ss.android.ugc.bytex.common.graph.cache

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ss.android.ugc.bytex.common.graph.ClassEntity
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.graph.IGraphCache
import com.ss.android.ugc.bytex.common.log.LevelLog
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import java.io.*
import java.lang.reflect.Modifier
import java.util.stream.Collectors

/**
 * Created by yangzhiqian on 2019-12-01<br/>
 */
object GsonGraphCache : IGraphCache<File> {
    private val GSON by lazy {
        GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(GraphTypeAdapterFactory())
                .create()
    }
    private var ramCaches: RAMGraphCache? = null
    var asyncSaveCache = true

    fun useRamCache(use: Boolean) {
        ramCaches = if (use) {
            RAMGraphCache
        } else {
            RAMGraphCache.clear()
            null
        }
    }

    override fun loadCache(t: File?, graphBuilder: GraphBuilder): Boolean {
        if (t == null) {
            return false
        }
        try {
            if (ramCaches?.loadCache(t, graphBuilder) == true) {
                println("Load ByteX Cache Success:from RAM")
                return true
            }
            if (!t.exists() || !t.isFile) {
                return false
            }
            BufferedReader(FileReader(t)).use { reader ->
                GSON.fromJson<List<ClassEntity>>(reader, object : TypeToken<List<ClassEntity>>() {
                }.type).apply {
                    //经过抖音cache测试性能,结果多线程方案差不多，但大约结果是
                    // Schedulers.COMPUTATION().submitAndAwait<parallelStream().forEach<forEach
                    forEach {
                        graphBuilder.add(it)
                    }
                }
            }
            println("Load ByteX Cache Success:" + t.absolutePath)
            return true
        } catch (e: Exception) {
            t.delete()
            println("Load ByteX Cache Fail:" + t.absolutePath)
            LevelLog.sDefaultLogger.e("loadCache failure", e)
        }
        return false
    }

    override fun saveCache(t: File?, graph: Graph): Boolean {
        if (t == null) {
            return false
        }
        try {
            if (ramCaches?.saveCache(t, graph) == true) {
                println("Save ByteX Cache Succeed:RAW")
            }
            graph.nodes.values.stream()
                    .map { node -> node.entity }
                    .collect(Collectors.toList<ClassEntity>())
                    .let { list ->
                        t.parentFile.mkdirs()
                        t.delete()
                        t.createNewFile()
                        Schedulers.IO().submit {
                            BufferedWriter(FileWriter(t)).use { writer ->
                                GSON.toJson(list, writer)
                                writer.flush()
                            }
                            println("Save ByteX Cache Succeed:" + t.absolutePath)
                        }
                    }.apply {
                        if (!asyncSaveCache) {
                            get()
                        }
                    }
            return true
        } catch (e: Exception) {
            t.delete()
            e.printStackTrace()
            LevelLog.sDefaultLogger.e("Save ByteX Cache failure", e)
        }
        return false
    }
}