package com.boostvision.platform.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class LiveDataCallFactory private constructor() : CallAdapter.Factory() {
    companion object {
        fun create(): LiveDataCallFactory {
            return LiveDataCallFactory()
        }
    }

    override fun get(
            returnType: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != LiveData::class.java) {
            return null
        }

        isTypeIllige(returnType)
        returnType as ParameterizedType
        val responseResultType = getParameterUpperBound(0, returnType)
        return LiveDataResponseCallAdapter<Nothing>(responseResultType)
    }

    private fun isTypeIllige(type: Type) {
        if (type !is ParameterizedType) {
            throwError()
        }
    }

    private fun throwError() {
        throw IllegalStateException("Response must be parametrized as " + "LiveData<ResposeResultWithErrorType<DataT,ErrorT>> or LiveData<ResposeResult<DataT>> or LiveData<DataT>")
    }

    inner class LiveDataResponseCallAdapter<T> internal constructor(
            private val responseType: Type
    ) : CallAdapter<T, LiveData<T>> {

        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<T>): LiveData<T> {
            val liveDataResponse = MutableLiveData<T>()
            call.enqueue(LiveDataResponseCallback(liveDataResponse))
            return liveDataResponse
        }

        private inner class LiveDataResponseCallback internal constructor(private val liveData: MutableLiveData<T>) : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (call.isCanceled) return
                if (response.isSuccessful) {
                    try {
                        liveData.postValue(response.body())
                    } catch (throwale: Throwable) {
                        onFailure(call, throwale)
                    }
                } else {
                    liveData.postValue(ResponseBean(null, null, 1) as T)
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun onFailure(call: Call<T>, t: Throwable) {
                if (call.isCanceled) return
                liveData.postValue(ResponseBean(null, null, 1) as T)
            }
        }
    }


}
