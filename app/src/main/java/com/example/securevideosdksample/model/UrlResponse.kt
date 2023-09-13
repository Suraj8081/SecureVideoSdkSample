package com.example.securevideosdksample.model

import com.example.securevideosdksample.model.Meta
import com.google.gson.annotations.SerializedName

data class UrlResponse(@SerializedName("meta")
                       val meta: Meta,
                       @SerializedName("width")
                       val width: String = "",
                       @SerializedName("filesize")
                       val filesize: String = "",
                       @SerializedName("label")
                       val label: String = "",
                       @SerializedName("type")
                       val type: String = "",
                       @SerializedName("url")
                       val url: String = "",
                       @SerializedName("height")
                       val height: String = "")