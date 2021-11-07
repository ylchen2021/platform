package com.boostvision.platform.media.mirror

data class MirrorEvent(
    val eventType: MirrorEventType,
    val status: MirrorStatus?,
    val param: Any?
)

enum class MirrorEventType {
    STATUS,
    ERROR
}

enum class MirrorStatus {
    DISCONNECTED,
    UPLOADING,
    CONNECTING,
    MIRRORING,
}