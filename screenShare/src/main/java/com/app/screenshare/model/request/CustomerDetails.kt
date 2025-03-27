package com.app.screenshare.model.request

import com.google.gson.annotations.SerializedName


data class CustomerDetails (

  @SerializedName("name"  ) var name  : String? = null,
  @SerializedName("email" ) var email : String? = null

)