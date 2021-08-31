package com.boostvision.platform.media.miracast

import android.os.Handler
import android.view.Surface
import com.boostvision.platform.utils.ReflectTool
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


class RemoteDisplayReflect private constructor(private var obj: Any?){

    companion object {
        @JvmStatic
        fun listen(iface: String, listener: Listener, handler: Handler, packageName: String): RemoteDisplayReflect {
            var ListenerClazz = Class.forName("android.media.RemoteDisplay${'$'}Listener")

            val listenerProxy = Proxy.newProxyInstance(
                ListenerClazz.getClassLoader(),
                arrayOf<Class<*>>(ListenerClazz),
                RemoteDisplayListenerImpl(listener))

            var obj = ReflectTool.callStaticMethod("android.media.RemoteDisplay", "listen", iface, listenerProxy, handler, packageName)
            return RemoteDisplayReflect(obj)
        }
    }

    fun resume() {
        ReflectTool.callMethod(obj, "resume")
    }

    fun pause() {
        ReflectTool.callMethod(obj, "pause")
    }

    fun dispose() {
        ReflectTool.callMethod(obj, "dispose")
    }

    interface Listener {
        fun onDisplayConnected(
            surface: Surface?,
            width: Int, height: Int, flags: Int, session: Int
        )

        fun onDisplayDisconnected()
        fun onDisplayError(error: Int)
    }

    class RemoteDisplayListenerImpl(private var listener: Listener): InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
            if (method.name == "onDisplayConnected") {
                var surface = args[0] as Surface
                var width = args[1] as Int
                var heigh = args[2] as Int
                var flags = args[3] as Int
                var session = args[4] as Int
                listener.onDisplayConnected(surface, width, heigh, flags, session)
            } else if (method.name == "onDisplayDisconnected") {
                listener.onDisplayDisconnected()
            } else if (method.name == "onDisplayError") {
                var error = args[0] as Int
                listener.onDisplayError(error)
            }
            return proxy
        }
    }
}