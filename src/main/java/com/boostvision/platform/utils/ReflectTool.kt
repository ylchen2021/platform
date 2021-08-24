package com.boostvision.platform.utils

object ReflectTool {
    fun getField(target: Any, fieldName: String): Any? {
        try {
            var clazz = target.javaClass
            var field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            return field.get(target)
        } catch (e: Exception) {
            e.printStackTrace();
            return null
        }
    }
}