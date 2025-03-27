package com.app.screenshare.model.request

import com.google.gson.annotations.SerializedName


data class CreateSessionRequest (

  @SerializedName("api_key"         ) var apiKey          : String?          = null,
  @SerializedName("deviceInfo"      ) var deviceInfo      : DeviceInfo?      = DeviceInfo(),
  @SerializedName("customerDetails" ) var customerDetails : CustomerDetails? = CustomerDetails()

)