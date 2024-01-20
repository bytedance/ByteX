package com.ss.android.ugc.bytex.common.utils

import org.gradle.api.JavaVersion
import org.gradle.api.invocation.Gradle
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 用途：全局单例，忽略因为gradle的类加载器变化导致的全局单例泄露问题
 * 问题:每次修改buildSrc内的代码，都将导致gradle顶层的ClassLoader变化，同时导致插件的ClassLoader变化
 * 这将使得插件中的单例类被同一个gradle进程加载两次，如果此时有插件想用单例做daemon进程编译复用，此种状态
 * 单例数据将失效，并造成内存泄露。
 * 这里使用比jre的类加载器来存，利用其中的parallelLockMap对象，按照加上ClassLoader的过滤对单例数据进行
 * 加载，同时在ClassLoader变化的情况下，将之前的缓存失效，做到真正的全局单例唯一。
 */
object GradleDaemonIgnoreClassLoaderSingletonManager {
    private val map = Gradle::class.java.classLoader.let { classLoader ->
        val field = if (JavaVersion.current().ordinal >= JavaVersion.VERSION_12.ordinal) {
            /**
             * JDK17针对一些类增加了反射限制, 这里特殊处理下，否则会有如下报错：
             * Caused by: java.lang.NoSuchFieldException: parallelLockMap
             * 同时，虚拟机参数需要增加如下配置(可加在org.gradle.jvmargs里):
             * --add-opens=java.base/java.lang=ALL-UNNAMED
             *
             * About the feature "Filtering for classes with security sensitive fields":
             * Core reflection has a filtering mechanism to hide security and integrity sensitive
             * fields and methods from Class getXXXField(s) and getXXXMethod(s).
             * The filtering mechanism has been used for several releases to hide security sensitive
             * fields such as System.security and Class.classLoader.
             * This CSR proposes to extend the filters to hide fields from a number of highly
             * security sensitive classes in java.lang.reflect and java.lang.invoke.
             *
             * You can visit https://bugs.openjdk.org/browse/JDK-8210522 for more details.
             */
            val getDeclaredFields0: Method = Class::class.java.getDeclaredMethod(
                "getDeclaredFields0",
                Boolean::class.javaPrimitiveType
            )
            getDeclaredFields0.isAccessible = true
            (getDeclaredFields0.invoke(ClassLoader::class.java, false) as Array<Field>)
                .first { "parallelLockMap" == it.name }
        } else {
            ClassLoader::class.java.getDeclaredField("parallelLockMap")
        }
        field.isAccessible = true
        field.get(classLoader) as MutableMap<Any, Any>
    }

    @Synchronized
    fun put(context: Any, key: String, value: Any) {
        map.put(key, Pair(context.javaClass, value))
    }

    @Synchronized
    fun <T> get(context: Any, key: String, clearIfClassLoaderChanged: Boolean = true): T? {
        val pair = (map.get(key) as? Pair<*, *>) ?: return null
        if (pair.first == context.javaClass) {
            //classloader可用
            return pair.second as T
        } else {
            if (clearIfClassLoaderChanged) {
                map.remove(key)
            }
            return null
        }
    }

    @Synchronized
    fun <T> computeIfAbsent(context: Any, key: String, function: (Any) -> T): T {
        val get = get<T>(context, key)
        if (get == null) {
            put(context, key, function.invoke(key) as Any)
            return get<T>(context, key) as T
        } else {
            return get
        }
    }

    @Synchronized
    fun remove(key: String): Any? {
        return map.remove(key)
    }
}