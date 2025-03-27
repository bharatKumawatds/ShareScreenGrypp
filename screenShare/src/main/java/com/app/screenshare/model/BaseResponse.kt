package com.app.screenshare.model

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: T
)