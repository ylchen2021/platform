package remote.common.network

import remote.common.network.ErrorType.SUCCESS

data class ResponseBean<T> (
    val data: T?,
    val errorMessage: String? = null,
    val status: Int? = 0,
) {
    fun isSuccess(): Boolean {
        return status == SUCCESS
    }
}


