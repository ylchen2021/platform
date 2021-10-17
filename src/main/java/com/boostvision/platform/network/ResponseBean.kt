package com.boostvision.platform.network

data class ResponseBean<T> (
    val data: T?,
    val errorMessage: String? = null,
    val status: Int? = 0,
) {
    fun isSuccess(): Boolean {
        return status == 0
    }
}


