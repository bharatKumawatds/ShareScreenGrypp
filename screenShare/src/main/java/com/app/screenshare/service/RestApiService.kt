package com.app.screenshare.service


import com.app.screenshare.model.BaseResponse
import com.app.screenshare.model.request.CreateSessionRequest
import retrofit2.Call
import retrofit2.http.Body

import retrofit2.http.POST


/**
 * Created by Nsikak  Thompson on 3/11/2017.
 */
interface RestApiService {
    @POST("/createSession")
    fun createSession(@Body createSessionRequest: CreateSessionRequest): Call<BaseResponse<Any>>
}