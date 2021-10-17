package com.boostvision.platform.adb

data class AdbEvent(
    val eventType: AdbEventType,
    val param: Any
)

enum class AdbEventType {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
    RESPONSE,
    FILE_PUSHED
}