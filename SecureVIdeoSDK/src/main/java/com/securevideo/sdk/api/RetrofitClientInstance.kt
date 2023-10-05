package com.securevideo.sdk.api

import androidx.multidex.MultiDexApplication
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal class RetrofitClientInstance : MultiDexApplication() {
    companion object {
        private var retrofit: Retrofit? = null
        private val httpClient =
            OkHttpClient.Builder().proxy(Proxy.NO_PROXY).connectTimeout(60, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.MINUTES)

        fun getRetrofitInstance(url: String): APIInterface {
            if (retrofit == null) {
                val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                val interceptor = HttpLoggingInterceptor()
                interceptor.setLevel(HttpLoggingInterceptor.Level.NONE)
                val client =
                    httpClient.addInterceptor(interceptor).connectTimeout(60, TimeUnit.MINUTES)
                        .readTimeout(60, TimeUnit.MINUTES).writeTimeout(1, TimeUnit.MINUTES).build()
                retrofit = Retrofit.Builder().client(client).baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create(gson)).build()
            }
            return retrofit!!.create(APIInterface::class.java)
        }

    }
}