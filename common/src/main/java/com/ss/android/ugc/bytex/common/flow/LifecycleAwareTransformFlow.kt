package com.ss.android.ugc.bytex.common.flow

import com.ss.android.ugc.bytex.common.graph.Graph
import com.ss.android.ugc.bytex.common.utils.executeWithStartAndFinish
import java.util.*
import java.util.function.Consumer

/**
 * Created by yangzhiqian on 2020/8/25<br/>
 */

internal class LifecycleAwareTransformFlow(private val real: TransformFlow) : TransformFlow by real {
    private var listenerManager = TransformFlowListenerManager()

    init {
        TransformFlow::class.java.methods.filter {
            it.isDefault
        }.forEach {
            try {
                LifecycleAwareTransformFlow::class.java.getDeclaredMethod(it.name, *it.parameterTypes)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("default methods in java interface must be overridden while using `by operation` in kotlin", e)
            }
        }
    }

    override fun prepare() {
        executeWithStartAndFinish(
                { real.prepare() },
                { listenerManager.startPrepare(real) },
                { e -> listenerManager.finishPrepare(real, e) }
        )
    }

    override fun run() {
        executeWithStartAndFinish(
                { real.run() },
                { listenerManager.startRunning(real, false) },
                { e -> listenerManager.finishRunning(real, e) }
        )
    }


    override fun name(): String {
        return real.name()
    }

    override fun getClassGraph(): Graph? {
        return real.classGraph
    }

    override fun getPriority(): Int {
        return real.priority
    }

    override fun registerTransformFlowListener(listener: TransformFlowListener) {
        listenerManager.registerTransformFlowListener(listener)
    }

    override fun isLifecycleAware(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return real.equals(other)
    }

    override fun hashCode(): Int {
        return real.hashCode()
    }

    override fun toString(): String {
        return real.toString()
    }

    override fun iterator(): MutableIterator<TransformFlow> {
        return real.iterator()
    }

    override fun forEach(p0: Consumer<in TransformFlow>?) {
        real.forEach(p0)
    }

    override fun spliterator(): Spliterator<TransformFlow> {
        return real.spliterator()
    }
}