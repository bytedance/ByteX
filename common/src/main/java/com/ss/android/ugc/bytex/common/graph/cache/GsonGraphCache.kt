package com.ss.android.ugc.bytex.common.graph.cache

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ss.android.ugc.bytex.common.graph.ClassEntity
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.graph.IGraphCache
import com.ss.android.ugc.bytex.common.log.LevelLog
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
                .disableHtmlEscaping().create()
    }

    override fun loadCache(t: File?, graphBuilder: GraphBuilder): Boolean {
        if (t == null || !t.exists() || !t.isFile) {
            return false
        }
        try {
            System.out.println("Load ByteX Cache:" + t.absolutePath)
            BufferedReader(FileReader(t)).use { reader ->
                GSON.fromJson<List<ClassEntity>>(reader, object : TypeToken<List<ClassEntity>>() {
                }.type).parallelStream().forEach { graphBuilder.add(it) }
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
            t.parentFile.mkdirs()
            t.delete()
            t.createNewFile()
            BufferedWriter(FileWriter(t)).use { writer ->
                GSON.toJson(graph.nodes.values.stream().map { node -> node.entity }.collect(Collectors.toList<ClassEntity>()), writer)
                writer.flush()
            }
            return true
        } catch (e: Exception) {
            t.delete()
            e.printStackTrace()
            LevelLog.sDefaultLogger.e("saveCache failure", e)
        }
        return false
    }
}