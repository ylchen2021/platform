package com.boostvision.platform.utils

import java.lang.reflect.InvocationTargetException

object ReflectTool {
    fun setField(target: Any?, fieldName: String, obj: Any?) {
        if (target == null) {
            return
        }
        var cls: Class<*>? = target.javaClass
        while (cls != null) {
            try {
                val field = cls.getDeclaredField(fieldName)
                field.isAccessible = true
                field[target] = obj
                return
            } catch (e: NoSuchFieldException) {
            } catch (e: IllegalAccessException) {
            }
            cls = cls.superclass
        }
    }

    fun getField(target: Any?, fieldName: String): Any? {
        if (target == null) {
            return null
        }
        return try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field[target]
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            null
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            null
        }
    }

    fun callMethod(target: Any?, methodName: String, vararg objects: Any?): Any? {
        if (target == null) {
            return null
        }
        try {
            val methods = target.javaClass.declaredMethods
            for (method in methods) {
                if (method.name == methodName) {
                    method.isAccessible = true
                    return method.invoke(target, *objects)
                }
            }
            NoSuchMethodException().printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return null
    }

    fun callStaticMethod(className: String, methodName: String, vararg objects: Any?): Any? {
        try {
            val clazz = Class.forName(className)
            val methods = clazz.declaredMethods
            for (method in methods) {
                if (method.name == methodName) {
                    method.isAccessible = true
                    return method.invoke(null, *objects)
                }
            }
            NoSuchMethodException().printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return null
    }
}