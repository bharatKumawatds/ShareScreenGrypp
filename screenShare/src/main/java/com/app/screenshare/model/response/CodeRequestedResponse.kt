package com.app.screenshare.model.response

import com.google.gson.annotations.SerializedName

data class CodeRequestedResponse (
    @SerializedName("value" ) var value : String? = "",
)