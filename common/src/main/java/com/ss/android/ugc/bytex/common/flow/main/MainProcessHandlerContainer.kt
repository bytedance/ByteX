package com.ss.android.ugc.bytex.common.flow.main

import java.util.*

/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */

class MainProcessHandlerContainer : Collection<MainProcessHandler> {
    private val handlers: MutableList<MainProcessHandler> = LinkedList<MainProcessHandler>()

    override val size: Int
        get() = handlers.size

    override fun contains(element: MainProcessHandler): Boolean {
        return element in handlers
    }

    override fun containsAll(elements: Collection<MainProcessHandler>): Boolean {
        return handlers.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return handlers.isEmpty()
    }

    override fun iterator(): Iterator<MainProcessHandler> {
        return handlers.iterator()
    }

    @Synchronized
    fun add(element: MainProcessHandler): Boolean {
        return handlers.add(LifecycleAwareManProcessHandler(element))
    }

    @Synchronized
    fun remove(element: MainProcessHandler): Boolean {
        return handlers.remove(element)
    }

    @Synchronized
    fun clear() {
        handlers.clear()
    }
}