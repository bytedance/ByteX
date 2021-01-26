package com.ss.android.ugc.bytex.gradletoolkit

import java.lang.reflect.Field
import java.lang.reflect.Modifier

internal object ReflectionUtils {
    fun <T> getField(caller: Any?, clazz: Class<*>, name: String): T {
        return clazz.getDeclaredField(name).let {
            it.isAccessible = true
            it.get(caller)
        } as T
    }

    fun setField(caller: Any?, clazz: Class<*>, name: String, value: Any?) {
        return clazz.getDeclaredField(name).let {
            it.isAccessible = true
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            val originalModifier: Int = it.modifiers
            try {
                modifiersField.setInt(it, originalModifier and Modifier.FINAL.inv())
                it.set(caller, value)
            } finally {
                modifiersField.setInt(it, originalModifier)
            }
        }
    }

    fun <T> callMethod(caller: Any?, clazz: Class<*>, name: String, argsType: Array<Class<*>>, args: Array<Any?>): T {
        return clazz.getDeclaredMethod(name, *argsType).let {
            it.isAccessible = true
            it.invoke(caller, *args)
        } as T
    }

    fun <T> callPublicMethod(caller: Any?, clazz: Class<*>, name: String, argsType: Array<Class<*>>, args: Array<Any?>): T {
        return clazz.getMethod(name, *argsType).let {
            it.isAccessible = true
            it.invoke(caller, *args)
        } as T
    }
}