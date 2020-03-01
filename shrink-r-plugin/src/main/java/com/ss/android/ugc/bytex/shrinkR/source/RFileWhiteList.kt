package com.ss.android.ugc.bytex.shrinkR.source

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Created by yangzhiqian on 2020-02-19<br/>
 * Desc:
 */
class RFileWhiteList(whiteList: Set<String>) {
    private val typeMap = HashMap<String, MutableSet<String>>()
    private val nameSet = HashSet<String>()

    init {
        whiteList.map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach {
                    val index = it.indexOf("/")
                    if (index > 0) {
                        typeMap.computeIfAbsent(it.substring(0, index).trim()) { HashSet() }.add(it.substring(index + 1, it.length).trim())
                    } else {
                        nameSet.add(it)
                    }
                }
    }

    fun inWhiteList(className: String, fieldName: String): Boolean {
        if (nameSet.contains(fieldName)) {
            return true
        }
        return typeMap[getRealRClassName(className)]?.contains(fieldName) ?: false
    }

    companion object {
        private val pattern = Pattern.compile("(^.*)(_\\d+)$")
        private val realRClassNameCache: MutableMap<String, String> = ConcurrentHashMap()
        fun getRealRClassName(className: String): String {
            return realRClassNameCache.computeIfAbsent(className) { origin ->
                val matcher = pattern.matcher(origin);
                if (matcher.matches()) {
                    matcher.group(1);
                } else {
                    origin;
                }
            }
        }
    }
}