package com.ss.android.ugc.bytex.common.graph.cache

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ss.android.ugc.bytex.common.graph.ClassEntity
import com.ss.android.ugc.bytex.common.log.LevelLog
import java.io.*
import java.lang.reflect.Modifier

/**
 * Created by yangzhiqian on 2019-12-01<br/>
 */
class GsonClassesCache(private val delegate: ICache<File, List<ClassEntity>>? = null) : IClassCache {
    companion object {
        private val GSON by lazy {
            GsonBuilder()
                    .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                    .disableHtmlEscaping()
                    .registerTypeAdapterFactory(GraphTypeAdapterFactory())
                    .create()
        }
    }

    override fun loadCache(t: File?): List<ClassEntity>? {
        delegate?.loadCache(t)?.apply { return this }
        if (t == null) {
            return null
        }
        try {
            if (!t.exists() || !t.isFile) {
                return null
            }
            BufferedReader(FileReader(t)).use { reader ->
                return GSON.fromJson<List<ClassEntity>>(reader, object : TypeToken<List<ClassEntity>>() {
                }.type).apply {
                    println("Load ByteX Classes Cache Success[File]:" + t.absolutePath)
                }
            }
        } catch (e: Exception) {
            t.delete()
            println("Load ByteX Classes Cache Fail[File]:" + t.absolutePath)
            LevelLog.sDefaultLogger.e("loadCache failure", e)
        }
        return null
    }

    override fun saveCache(t: File?, d: List<ClassEntity>): Boolean {
        delegate?.saveCache(t, d)
        if (t == null) {
            return false
        }
        t.parentFile.mkdirs()
        t.delete()
        t.createNewFile()
        BufferedWriter(FileWriter(t)).use { writer ->
            GSON.toJson(d, writer)
            writer.flush()
        }
        println("Save ByteX Classes Cache Succeed[File]:" + t.absolutePath)
        return true
    }
}