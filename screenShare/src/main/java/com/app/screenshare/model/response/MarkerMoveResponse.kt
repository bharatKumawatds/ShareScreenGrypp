package com.app.screenshare.model.response

import com.google.gson.annotations.SerializedName

data class MarkerMoveResponse (
    @SerializedName("action" ) var action : String? = null,
    @SerializedName("value"  ) var value  : MarkerValue?  = MarkerValue()
)
