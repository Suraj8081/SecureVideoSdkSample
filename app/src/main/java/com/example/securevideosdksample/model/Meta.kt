package com.example.securevideosdksample.model

import com.google.gson.annotations.SerializedName

data class Meta(@SerializedName("password")
                var password: String = "",
                @SerializedName("download")
                var download: String = "")