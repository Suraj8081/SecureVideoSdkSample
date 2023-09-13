package com.example.securevideosdksample.model

import com.google.gson.annotations.SerializedName

data class Meta(@SerializedName("password")
                val password: String = "",
                @SerializedName("download")
                val download: String = "")