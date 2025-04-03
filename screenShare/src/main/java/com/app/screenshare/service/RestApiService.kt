package com.app.screenshare.service


import com.app.screenshare.model.response.CreateSessionResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header

import retrofit2.http.POST


interface RestApiService {


    @POST("create-session")
    suspend fun createSession(@Header("APIKey") apiKey:String): Response<CreateSessionResponse>

}