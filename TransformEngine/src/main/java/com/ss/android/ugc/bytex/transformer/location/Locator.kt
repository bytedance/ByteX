package com.ss.android.ugc.bytex.transformer.location

import com.ss.android.ugc.bytex.gradletoolkit.Artifact
import com.ss.android.ugc.bytex.transformer.TransformContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.stream.Stream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by yangzhiqian on 2019-12-10<br/>
 */
class Locator(private val context: TransformContext) {
    private val inputJarLocator by lazy {
        mutableMapOf<String, MutableList<Location.FileLocation>>().also { result ->
            context.allJars.stream().map { it.file }.forEach { jar ->
                traverseToFileLocation(jar).forEach { location ->
                    result.computeIfAbsent(location.key) { LinkedList() }.addAll(location.value)
                }
            }
        }
    }

    private val inputDirLocator by lazy {
        mutableMapOf<String, MutableList<Location.FileLocation>>().also { result ->
            context.allDirs.stream().map { it.file }.forEach { dir ->
                traverseToFileLocation(dir, Location.FileLocation(dir.absolutePath)).forEach { location ->
                    result.computeIfAbsent(location.key) { LinkedList() }.addAll(location.value)
                }
            }
        }
    }

    private val originJarLocator by lazy {
        mutableMapOf<String, MutableList<Location.FileLocation>>().also { result ->
            context.getArtifact(Artifact.JAR).stream().forEach { jar ->
                traverseToFileLocation(jar).forEach { location ->
                    result.computeIfAbsent(location.key) { LinkedList() }.addAll(location.value)
                }
            }
        }
    }

    private val originDirLocator by lazy {
        mutableMapOf<String, MutableList<Location.FileLocation>>().also { result ->
            context.getArtifact(Artifact.ALL_CLASSES).stream().forEach { dir ->
                traverseToFileLocation(dir, Location.FileLocation(dir.absolutePath)).forEach { location ->
                    result.computeIfAbsent(location.key) { LinkedList() }.addAll(location.value)
                }
            }
        }
    }

    private val originAarLocator by lazy {
        mutableMapOf<String, MutableList<Location.FileLocation>>().also { result ->
            context.getArtifact(Artifact.AAR).stream().forEach { dir ->
                traverseToFileLocation(dir).forEach { location ->
                    result.computeIfAbsent(location.key) { LinkedList() }.addAll(location.value)
                }
            }
        }
    }

    /**
     * Find the location of a file<br/>
     * @param path filePath.For example,you may wang to know the location of a class
     *  you should pass the full name of the class you find like 'okhttp3/internal/http2/Header.class'
     * @param scope search scope {@code SearchScope}
     *
     */
    fun findLocation(path: String, scope: SearchScope): Stream<Location.FileLocation>? {
        return when (scope) {
            SearchScope.INPUT -> Arrays.stream(arrayOf(inputDirLocator, inputJarLocator)).map { it[path] }.filter { it != null }.flatMap { it!!.stream() }
            SearchScope.INPUT_DIR -> inputDirLocator[path]?.stream()
            SearchScope.INPUT_JAR -> inputJarLocator[path]?.stream()
            SearchScope.ORIGIN -> Arrays.stream(arrayOf(originJarLocator, originDirLocator)).map { it[path] }.filter { it != null }.flatMap { it!!.stream() }
            SearchScope.ORIGIN_JAR -> originJarLocator[path]?.stream()
            SearchScope.ORIGIN_DIR -> originDirLocator[path]?.stream()
            SearchScope.ORIGIN_AAR -> originAarLocator[path]?.stream()
        }
    }

    private fun traverseToFileLocation(file: File, parentLocation: Location.FileLocation = Location.ROOT): Map<String, List<Location.FileLocation>> {
        if (!file.exists()) {
            return emptyMap()
        }
        val path = file.absolutePath.let { absolutePath ->
            if (absolutePath.startsWith(parentLocation.path + "/")) {
                absolutePath.substring(parentLocation.path.length + 1)
            } else {
                absolutePath
            }
        }
        val thisFileLocation = Location.FileLocation(path, parentLocation)
        val result = mutableMapOf<String, MutableList<Location.FileLocation>>()
        result.computeIfAbsent(path) { LinkedList() }.add(thisFileLocation)
        if (file.isFile) {
            if (isZipFile(file.name)) {
                ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zin ->
                    var zipEntry = zin.nextEntry
                    while (zipEntry != null) {
                        traverseEntryToFileLocation(zin, zipEntry, thisFileLocation).forEach {
                            result.computeIfAbsent(it.key) { LinkedList() }.addAll(it.value)
                        }
                        zipEntry = zin.nextEntry
                    }
                }
            }
            return result
        }
        if (file.isDirectory) {
            file.listFiles().forEach { child ->
                traverseToFileLocation(child, parentLocation).forEach {
                    result.computeIfAbsent(it.key) { LinkedList() }.addAll(it.value)
                }
            }
        }
        return result
    }

    private fun traverseEntryToFileLocation(zin: ZipInputStream, entry: ZipEntry, parentLocation: Location.FileLocation): Map<String, List<Location.ZipEntryLocation>> {
        val thisFileLocation = Location.ZipEntryLocation(parentLocation, entry.name)
        val result = mutableMapOf<String, MutableList<Location.ZipEntryLocation>>()
        result.computeIfAbsent(entry.name) { LinkedList() }.add(thisFileLocation)
        if (isZipFile(entry.name)) {
            ZipInputStream(zin).also { newZin ->
                var zipEntry = newZin.nextEntry
                while (zipEntry != null) {
                    traverseEntryToFileLocation(newZin, zipEntry, thisFileLocation).forEach {
                        result.computeIfAbsent(it.key) { LinkedList() }.addAll(it.value)
                    }
                    zipEntry = newZin.nextEntry
                }
            }
        }
        return result
    }

    private fun isZipFile(name: String): Boolean {
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".apk") || name.endsWith(".aar")
    }
}