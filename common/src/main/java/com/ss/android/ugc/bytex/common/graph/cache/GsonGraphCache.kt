package com.ss.android.ugc.bytex.common.graph.cache

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ss.android.ugc.bytex.common.graph.ClassEntity
import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.graph.IGraphCache
import com.ss.android.ugc.bytex.common.log.LevelLog
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Modifier
import java.util.function.Consumer
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
            val classEntities = GSON.fromJson<List<ClassEntity>>(t.readText(), object : TypeToken<List<ClassEntity>>() {
            }.type)
            classEntities.parallelStream().forEach(Consumer<ClassEntity> { graphBuilder.add(it) })
            System.out.println("Load ByteX Cache Success:" + t.absolutePath)
            return true
        } catch (e: FileNotFoundException) {
            System.out.println("Load ByteX Cache Fail:" + t.absolutePath)
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
            t.writeText(GSON.toJson(graph.nodes.values.stream().map { node -> node.entity }.collect(Collectors.toList<ClassEntity>())))
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            LevelLog.sDefaultLogger.e("saveCache failure", e)
        }
        return false
    }
}