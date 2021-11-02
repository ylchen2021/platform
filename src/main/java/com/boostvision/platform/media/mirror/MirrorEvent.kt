package com.boostvision.platform.media.mirror

data class MirrorEvent(
    val eventType: MirrorEventType,
    val status: MirrorStatus?,
    val param: Any?
)

enum class MirrorEventType {
    STATUS
}

enum class MirrorStatus {
    DISCONNECTED,
    CONNECTING,
    MIRRORING,
}