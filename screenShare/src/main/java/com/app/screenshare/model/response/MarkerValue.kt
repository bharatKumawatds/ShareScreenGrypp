package com.app.screenshare.model.response

import com.google.gson.annotations.SerializedName


data class MarkerValue (

  @SerializedName("userName" ) var userName : String? = null,
  @SerializedName("x"        ) var x        : Int?    = null,
  @SerializedName("y"        ) var y        : Int?    = null,
  @SerializedName("scale"    ) var scale    : Int?    = null

)