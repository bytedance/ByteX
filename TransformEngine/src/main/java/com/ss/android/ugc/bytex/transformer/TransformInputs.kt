package com.ss.android.ugc.bytex.transformer

import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.google.common.collect.Streams
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.ss.android.ugc.bytex.transformer.cache.*
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import com.ss.android.ugc.bytex.transformer.utils.getStack
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.ArrayList

/**
 * Created by yangzhiqian on 2020-03-05<br/>
 * Desc:
 */

class TransformInputs internal constructor(private val context: TransformContext,
                                           private val invocation: TransformInvocation,
                                           private val cacheFile: File,
                                           private val transformOptions: TransformOptions) {

    companion object {
        internal val gson by lazy { GsonBuilder().registerTypeAdapter(Entry::class.java, Entry.EntryTypeAdapter()).create() }
        internal val caches = ConcurrentHashMap<String, List<Entry>>()

    }

    private var allJars = ArrayList<JarCache>(invocation.inputs.size)
    private var allDirs = ArrayList<DirCache>(invocation.inputs.size)
    private var newDirs = ConcurrentHashMap<String, NewFileCache>()
    val lastTransformInputs by lazy {
        /*
        * 判断增量
        * 内存加载
        * 文件加载
        * */
        try {
            val map = mutableMapOf<String, Set<String>>()
            if (!invocation.isIncremental) {
                emptyList()
            } else {
                if (transformOptions.isUseRawCache) {
                    caches.remove(cacheFile.absolutePath)
                } else {
                    null
                } ?: if (cacheFile.isFile && cacheFile.exists()) {
                    BufferedReader(FileReader(cacheFile)).use {
                        gson.fromJson<List<Entry>>(it, object : TypeToken<ArrayList<Entry>>() {}.type)
                    }
                } else {
                    emptyList()
                }
            }.forEach {
                map[it.parent] = it.items.toSet()
            }
            Collections.unmodifiableMap(map)
        } finally {
            //用完即删
            cacheFile.delete()
            caches.remove(cacheFile.absolutePath)
        }
    }

    val transformInputs by lazy {
        val inputs = ConcurrentHashMap<String, List<FileData>>()
        allFiles().parallel().forEach {
            it.stream().filter { it.status != Status.REMOVED }.toList().blockingGet().apply {
                if (isNotEmpty()) {
                    inputs[it.file.absolutePath] = this
                }
            }
        }
        Collections.unmodifiableMap(inputs)
    }

    val changedFiles by lazy {
        allFiles().flatMap { fileCache: FileCache -> fileCache.changedFiles.stream() }.collect(Collectors.toList())
    }

    init {
        invocation.inputs.forEach(Consumer { input: TransformInput ->
            input.jarInputs.forEach { allJars.add(JarCache(it, context, transformOptions.isUseFixedTimestamp)) }
            input.directoryInputs.forEach { allDirs.add(DirCache(it, context)) }
        })
        //提前加载
        Schedulers.IO().submit {
            println("lastTransformInputs size:${lastTransformInputs.size}")
        }
    }

    fun getAllDirs(): Collection<DirCache> {
        return Collections.unmodifiableCollection(allDirs)
    }

    fun getAllJars(): Collection<JarCache> {
        return Collections.unmodifiableCollection(allJars)
    }

    fun allFiles(): Stream<FileCache> {
        return Streams.concat(allDirs.stream(), allJars.stream(), newDirs.values.stream())
    }

    protected fun requestNotIncremental() {
        println("ByteX requestNotIncremental(${getCallStack()})")
        transformInputs.flatMap { it.value }.forEach {
            if (it.status == Status.NOTCHANGED) {
                it.status = Status.CHANGED
            }
        }
    }

    protected fun requestNotIncremental(relativePath: String): Boolean {
        println("ByteX requestNotIncremental(${getCallStack()}):$relativePath")
        var r = false
        transformInputs.flatMap { it.value }.forEach {
            if (it.relativePath == relativePath && it.status == Status.NOTCHANGED) {
                it.status = Status.CHANGED
                r = true
            }
        }
        return r
    }

    private fun getCallStack(): String {
        return getStack(1, "com.ss.android.ugc.bytex.transformer")
    }

    fun addFile(affinity: String, file: FileData) {
        newDirs.computeIfAbsent(affinity) {
            NewFileCache(context, affinity)
        }.addFile(file)
    }

    protected fun saveCache() {
        if (transformOptions.isShouldSaveCache) {
            val list = transformInputs.map {
                Entry(it.key, it.value.map { it.relativePath })
            }.toList()
            if (transformOptions.isUseRawCache && !context.isDaemonSingleUse) {
                caches.put(cacheFile.absolutePath, list)
            }
            Schedulers.IO().submit {
                BufferedWriter(FileWriter(cacheFile)).use {
                    gson.toJson(list, it)
                }
            }
        }
    }

    protected fun release() {
        allJars.clear()
        allDirs.clear()
        newDirs.clear()
    }

    class Entry(var parent: String = "", var items: List<String> = emptyList()) {
        class EntryTypeAdapter : TypeAdapter<Entry>() {
            override fun write(out: JsonWriter, value: Entry) {
                out.beginObject()
                out.name("p").value(value.parent)
                out.name("i")
                out.beginArray()
                value.items.forEach {
                    out.value(it)
                }
                out.endArray()
                out.endObject()
            }

            override fun read(`in`: JsonReader): Entry {
                `in`.beginObject()
                var parent: String? = null
                var items: MutableList<String>? = null
                while (`in`.hasNext()) {
                    val peek = `in`.peek()
                    if (peek == JsonToken.END_OBJECT) {
                        break
                    }
                    when (`in`.nextName()) {
                        "p" -> parent = `in`.nextString()
                        "i" -> {
                            `in`.beginArray()
                            items = mutableListOf()
                            while (`in`.peek() != JsonToken.END_ARRAY) {
                                items.add(`in`.nextString())
                            }
                            `in`.endArray()
                        }
                        else -> throw RuntimeException()
                    }
                }
                `in`.endObject()
                return Entry(parent!!, Collections.unmodifiableList(items!!))
            }
        }
    }
}