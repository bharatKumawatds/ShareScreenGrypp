package com.app.screenshare.model.response

import com.google.gson.annotations.SerializedName


data class CreateSessionResponse (

  @SerializedName("sessionId"     ) var sessionId     : String? = null,
  @SerializedName("customerToken" ) var customerToken : String? = null,
  @SerializedName("sessionCode"   ) var sessionCode   : String? = null,
  @SerializedName("apiKey"        ) var apiKey        : String? = null,
  @SerializedName("branchName"    ) var branchName    : String? = null,
  @SerializedName("branchId"      ) var branchId      : String? = null

)