package com.example.mobile.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "http://10.109.110.27:5500"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    var token: String? = null
}
