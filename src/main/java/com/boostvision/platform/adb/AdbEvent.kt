package com.boostvision.platform.adb

data class AdbEvent(
    val eventType: AdbEventType,
    val status: AdbStatus?,
    val param: Any
)

enum class AdbEventType {
    STATUS,
    ERROR,
    RESPONSE,
    FILE_PUSHED
}

enum class AdbStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}