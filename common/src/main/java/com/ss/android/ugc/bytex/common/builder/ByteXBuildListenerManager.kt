package com.ss.android.ugc.bytex.common.builder

import com.ss.android.ugc.bytex.common.builder.internal.DefaultByteXBuildListener
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandlerListener
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandlerListenerManager
import java.util.*

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 */
object ByteXBuildListenerManager {
    private val listeners: MutableList<ByteXBuildListener> = LinkedList()

    init {
        registerByteXBuildListener(DefaultByteXBuildListener)
        registerMainProcessHandlerListener(DefaultByteXBuildListener)
    }

    @Synchronized
    fun registerByteXBuildListener(listener: ByteXBuildListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun unRegisterByteXBuildListener(listener: ByteXBuildListener) {
        listeners.remove(listener)
    }

    fun registerMainProcessHandlerListener(listener: MainProcessHandlerListener) {
        MainProcessHandlerListenerManager.registerMainProcessHandlerListener(listener)
    }

    fun unregisterMainProcessHandlerListener(listener: MainProcessHandlerListener) {
        MainProcessHandlerListenerManager.unregisterMainProcessHandlerListener(listener)
    }

    @Synchronized
    internal fun getByteXBuildListeners(): List<ByteXBuildListener> {
        return Collections.unmodifiableList(listeners)
    }
}