package com.boostvision.platform.subscribe

data class SubscribeEvent(
    val eventType: SubscribeEventType,
    val param: Any
)

enum class SubscribeEventType {
    PURCHASE_SUCCESS,
    ERROR,
}

