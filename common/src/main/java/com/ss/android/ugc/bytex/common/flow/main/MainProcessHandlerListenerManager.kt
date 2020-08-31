package com.ss.android.ugc.bytex.common.flow.main

import com.android.build.gradle.internal.utils.toImmutableList
import com.google.common.collect.ImmutableList
import java.util.*

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */
object MainProcessHandlerListenerManager {
    private val listeners: MutableList<MainProcessHandlerListener> = LinkedList()

    @Synchronized
    fun registerMainProcessHandlerListener(listener: MainProcessHandlerListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun unregisterMainProcessHandlerListener(listener: MainProcessHandlerListener) {
        listeners.remove(listener)
    }

    internal fun getMainProcessHandlerListeners(): ImmutableList<MainProcessHandlerListener> {
        return listeners.toImmutableList()
    }
}