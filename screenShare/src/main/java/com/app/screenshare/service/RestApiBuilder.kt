package com.app.screenshare.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RestApiBuilder {
    private val retrofit: Retrofit

    init {
        // Set up logging interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Logs request and response body
            // Use Level.HEADERS for headers only, or Level.BASIC for minimal info
        }

        // Add the interceptor to OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Build Retrofit with the custom OkHttpClient
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Attach the OkHttpClient with logging
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: RestApiService
        get() = retrofit.create(RestApiService::class.java)

    companion object {
        const val BASE_URL: String = "https://thirdparty.grypp.io/in-app-sessions/"
    }
}