package com.app.screenshare.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class RestApiBuilder {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: RestApiService
        get() = retrofit.create(RestApiService::class.java)

    companion object {
        const val BASE_URL: String = "https://api.github.com"
    }
}