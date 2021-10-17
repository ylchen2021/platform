package com.boostvision.platform.network

import android.text.TextUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.net.Proxy
import java.util.concurrent.TimeUnit

class Client {
    companion object {
        fun <T> create(serviceUrl: String, clz: Class<T>): T {
            return Retrofit.Builder()
                .baseUrl(serviceUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(LiveDataCallFactory.create())
                .client(createOkHttpClient())
                .build()
                .create(clz)
        }

        private fun createOkHttpClient(): OkHttpClient? {
            val builder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
            return builder.build()
        }
    }
}