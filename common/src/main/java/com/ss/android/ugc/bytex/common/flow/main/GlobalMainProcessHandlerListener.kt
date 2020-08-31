package com.ss.android.ugc.bytex.common.flow.main

import com.ss.android.ugc.bytex.transformer.TransformEngine


/**
 * Created by yangzhiqian on 2020/8/30<br/>
 */
internal object GlobalMainProcessHandlerListener : MainProcessHandlerListener {
    override fun finishTraverseIncremental(handlers: MutableCollection<MainProcessHandler>, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishTraverseIncremental(handlers, exception)
        }
    }

    override fun startStartRunning(handler: MainProcessHandler, transformer: TransformEngine) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startStartRunning(handler, transformer)
        }
    }

    override fun finishInit(handler: MainProcessHandler, transformer: TransformEngine, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishInit(handler, transformer, exception)
        }
    }

    override fun startTraverseIncremental(handlers: MutableCollection<MainProcessHandler>) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startTraverseIncremental(handlers)
        }
    }

    override fun startAfterTransform(handler: MainProcessHandler, transformer: TransformEngine) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startAfterTransform(handler, transformer)
        }
    }

    override fun startTransform(handlers: MutableCollection<MainProcessHandler>) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startTransform(handlers)
        }
    }

    override fun finishTraverseAndroidJar(handlers: MutableCollection<MainProcessHandler>, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishTraverseAndroidJar(handlers, exception)
        }
    }

    override fun startTraverse(handlers: MutableCollection<MainProcessHandler>) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startTraverse(handlers)
        }
    }

    override fun finishStartRunning(handler: MainProcessHandler, transformer: TransformEngine, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishStartRunning(handler, transformer, exception)
        }
    }

    override fun finishBeforeTransform(handler: MainProcessHandler, transformer: TransformEngine, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishBeforeTransform(handler, transformer, exception)
        }
    }

    override fun startInit(handler: MainProcessHandler, transformer: TransformEngine) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startInit(handler, transformer)
        }
    }

    override fun startBeforeTraverse(handler: MainProcessHandler, transformer: TransformEngine) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startBeforeTraverse(handler, transformer)
        }
    }

    override fun finishTraverse(handlers: MutableCollection<MainProcessHandler>, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishTraverse(handlers, exception)
        }
    }

    override fun finishAfterTransform(handler: MainProcessHandler, transformer: TransformEngine, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishAfterTransform(handler, transformer, exception)
        }
    }

    override fun startBeforeTransform(handler: MainProcessHandler, transformer: TransformEngine) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startBeforeTransform(handler, transformer)
        }
    }

    override fun startTraverseAndroidJar(handlers: MutableCollection<MainProcessHandler>) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.startTraverseAndroidJar(handlers)
        }
    }

    override fun finishBeforeTraverse(handler: MainProcessHandler, transformer: TransformEngine, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishBeforeTraverse(handler, transformer, exception)
        }
    }

    override fun finishTransform(handlers: MutableCollection<MainProcessHandler>, exception: Exception?) {
        for (listener in MainProcessHandlerListenerManager.getMainProcessHandlerListeners()) {
            listener.finishTransform(handlers, exception)
        }
    }
}