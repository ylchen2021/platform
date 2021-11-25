package remote.common.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class HttpClient {
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(9999, TimeUnit.SECONDS)
                .readTimeout(9999, TimeUnit.SECONDS)
                .callTimeout(9999, TimeUnit.SECONDS)
            return builder.build()
        }
    }
}