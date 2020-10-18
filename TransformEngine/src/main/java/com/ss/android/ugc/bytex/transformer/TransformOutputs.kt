package com.ss.android.ugc.bytex.transformer

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.ss.android.ugc.bytex.transformer.cache.FileData
import com.ss.android.ugc.bytex.transformer.concurrent.Schedulers
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * Created by yangzhiqian on 2020-03-10<br/>
 * Desc:
 */

class TransformOutputs internal constructor(private val context: TransformContext,
                                            private val invocation: TransformInvocation,
                                            private val cacheFile: File,
                                            private val transformOptions: TransformOptions) {

    companion object {
        internal val gson by lazy { GsonBuilder().registerTypeAdapter(Entry::class.java, Entry.EntryTypeAdapter()).create() }
        internal val caches = ConcurrentHashMap<String, List<Entry>>()

        @Throws(IOException::class)
        @JvmStatic
        fun getOutputTarget(root: File, relativePath: String): File {
            val target = File(root, relativePath.replace('/', File.separatorChar))
            if (!target.exists()) {
                Files.createParentDirs(target)
            }
            return target
        }
    }

    val lastTransformOutputs by lazy {
        /*
        * 判断增量
        * 内存加载
        * 文件加载
        * */
        try {
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
            }.let {
                val map = HashMap<String, Entry>()
                for (entry in it) {
                    map[entry.path] = entry
                }
                Collections.unmodifiableMap(map)
            }
        } finally {
            //用完即删
            cacheFile.delete()
            caches.remove(cacheFile.absolutePath)
        }
    }

    val transformOutputs = ConcurrentHashMap<String, Entry>()

    init {
        //提前加载
        Schedulers.IO().submit {
            println("lastTransformOutputs size:${lastTransformOutputs.size}")
        }
    }

    @Throws(IOException::class)
    fun getOutputFile(content: QualifiedContent): File {
        return getOutputFile(content, true)
    }

    @Throws(IOException::class)
    fun getOutputFile(content: QualifiedContent, createIfNeed: Boolean): File {
        val target = invocation.outputProvider.getContentLocation(content.name, content.contentTypes, content.scopes,
                if (content is JarInput) Format.JAR else Format.DIRECTORY)
        if (createIfNeed && !target.exists()) {
            Files.createParentDirs(target)
        }
        return target
    }

    @Throws(IOException::class)
    fun getOutputDir(affinity: String): File {
        val root = invocation.outputProvider.getContentLocation(affinity, setOf(QualifiedContent.DefaultContentType.CLASSES),
                TransformManager.SCOPE_FULL_PROJECT, Format.DIRECTORY)
        if (!root.exists()) {
            Files.createParentDirs(root)
        }
        return root
    }

    /**
     * 获取相对project的相对路径，如果没有相对路径则返回绝对路径
     */
    fun relativeToProject(file: File): String {
        return try {
            file.toRelativeString(context.project.projectDir)
        } catch (e: IllegalArgumentException) {
            file.absolutePath
        }
    }


    protected fun saveCache() {
        if (transformOptions.isShouldSaveCache) {
            val enties = transformOutputs.values.toList()
            if (transformOptions.isUseRawCache && !context.isDaemonSingleUse) {
                caches[cacheFile.absolutePath] = enties
            }
            Schedulers.IO().submit {
                BufferedWriter(FileWriter(cacheFile)).use {
                    gson.toJson(enties, it)
                }
            }
        }
    }

    protected fun release() {
    }

    /**
     * @param input 输入文件,如果是jar中或者attachment中的FileData则为空，否则为输入文件
     * @param path  输出的路径,relativePath
     * @param hash  对应输出文件的hash
     * @param extras attachment
     */
    class Entry(val input: String? = null, val path: String, val hash: Long, val extras: List<Entry>) : Comparable<Entry> {

        /**
         * 唯一标示，忽略hash碰撞
         */
        val identify by lazy {
            genIdentify()
        }

        private fun genIdentify(): Long {
            var r = if (hash == INVALID_HASH) {
                0L
            } else {
                input.hashCode() + path.hashCode() + hash
            }
            for (extra in extras) {
                r += extra.identify
            }
            return r
        }

        fun traverseAll(action: Consumer<Entry>) {
            action.accept(this)
            for (extra in extras) {
                extra.traverseAll(action)
            }
        }

        companion object {
            const val INVALID_HASH = Long.MAX_VALUE
            fun FileData.outputEntry(parent: String): Entry {
                val path = relativePath
                val subs = LinkedList<Entry>().let { items ->
                    traverseAttachmentOnly {
                        items.add(it.outputEntry("$parent/$relativePath"))
                    }
                    items.sort()
                    Collections.unmodifiableList(items)
                }
                val hash = if (status == Status.REMOVED || bytes == null || bytes.isEmpty()) {
                    INVALID_HASH
                } else {
                    hash(bytes)
                }
                return Entry(null, path, hash, subs)
            }

            fun hash(file: File): Long = hash(file.readBytes())
            fun hash(array: ByteArray): Long = Hashing.crc32c().hashBytes(array).asInt().toLong()
        }


        class EntryTypeAdapter : TypeAdapter<Entry>() {
            override fun write(out: JsonWriter, value: Entry) {
                out.beginObject()
                if (value.input != null) {
                    out.name("i").value(value.input)
                }
                out.name("p").value(value.path)
                out.name("h").value(value.hash)
                if (value.extras.isNotEmpty()) {
                    out.name("e")
                    out.beginArray()
                    value.extras.forEach {
                        write(out, it)
                    }
                    out.endArray()
                }
                out.endObject()
            }

            override fun read(`in`: JsonReader): Entry {
                `in`.beginObject()
                var input: String? = null
                var path: String? = null
                var hash: Long = 0
                val items = LinkedList<Entry>()
                while (`in`.hasNext()) {
                    val peek = `in`.peek()
                    if (peek == JsonToken.END_OBJECT) {
                        break
                    }
                    when (`in`.nextName()) {
                        "i" -> input = `in`.nextString()
                        "p" -> path = `in`.nextString()
                        "h" -> hash = `in`.nextLong()
                        "e" -> {
                            `in`.beginArray()
                            while (`in`.peek() != JsonToken.END_ARRAY) {
                                items.add(read(`in`))
                            }
                            `in`.endArray()
                        }
                        else -> throw RuntimeException()
                    }
                }
                `in`.endObject()
                return Entry(input, path!!, hash, Collections.unmodifiableList(items))
            }
        }

        override fun hashCode(): Int {
            return path.hashCode() + hash.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is Entry && other.path == path && other.hash == hash
        }

        override fun compareTo(other: Entry): Int = path.compareTo(other.path)
    }
}