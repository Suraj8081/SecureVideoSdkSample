package com.securevideo.sdk.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface APIInterface {
    @FormUrlEncoded
    @Headers("device-type:1","Version:1")
    @POST
    fun linkPrepare(
        @Header("Authorization") device_id: String?,
        @Header("Siteid") account_id: String?,
        @Field("media_id") mediaid: String?,
        @Field("extra") extra: String?,
        @Url addurl: String?
    ): Call<ResponseBody?>?
}