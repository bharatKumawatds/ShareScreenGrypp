package com.app.screenshare.model.request

import com.google.gson.annotations.SerializedName


data class DeviceInfo (

  @SerializedName("brand"         ) var brand         : String? = null,
  @SerializedName("modelNumber"   ) var modelNumber   : String? = null,
  @SerializedName("displayWidth"  ) var displayWidth  : String? = null,
  @SerializedName("displayHeight" ) var displayHeight : String? = null

)