package com.boostvision.platform.subscribe

object SubscribeManager {
    private var isVIP = false
    fun isVIP(): Boolean {
        return isVIP
    }

    fun enableVIP() {
        isVIP = true
    }
}