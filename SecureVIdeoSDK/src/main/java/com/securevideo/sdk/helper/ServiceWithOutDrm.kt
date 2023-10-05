package com.securevideo.sdk.helper

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.securevideo.sdk.model.UserDetails
import com.securevideo.sdk.api.RetrofitClientInstance
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class ServiceWithOutDrm(
    val context: Context,
    val initializeMyAppPlayer: InitializeMyAppPlayer?,
    userId: String,
    siteid: String,
    secretKey: String,
    mediaid: String,
    accessKey: String,
    isLive: Boolean
) {
    private var deviceName: String? = null
    private var version: String? = null

    init {
        deviceName = Build.MODEL
        version = getsdkverssion(context).toString()
        val SDK_INT = Build.VERSION.SDK_INT
        if (SDK_INT > 8) {
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
        var apiType = ""
        if (isLive) {
            apiType = "get-streaming-details"

            linkPrepare(userId, siteid, secretKey, mediaid, accessKey, apiType)
        } else {
            apiType = "get-video-meta"
            linkPrepare(userId, siteid, secretKey, mediaid, accessKey, apiType)
        }
    }

    companion object {
        const val baseUrl = "https://securevdo.com/"
        //////////////////////for vod///////////////

        //////////live///////////////
    }

    private fun getsdkverssion(activity: Context): Int {
        var version = 0
        try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            version = pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return version
    }

    private fun linkPrepare(
        userId: String,
        accountid: String,
        secretKey: String,
        mediaid: String,
        accessKey: String,
        apiType: String
    ) {
        try {
            val userdetail = UserDetails()
            userdetail.userid = userId
            userdetail.device = deviceName!!
            userdetail.version = version!!
            val data = Gson().toJson(userdetail)
            val encyData = AES.encrypt(data, accessKey)
            RetrofitClientInstance.getRetrofitInstance("$baseUrl/").let {
                it.linkPrepare(secretKey, accountid, mediaid, encyData, "$baseUrl${apiType}")
                    ?.enqueue(object : Callback<ResponseBody?> {
                        override fun onResponse(
                            call: Call<ResponseBody?>, response: Response<ResponseBody?>
                        ) {
                            try {
                                val decryptData = AES.decrypt(response.body()!!.string(), accessKey)
                                if (decryptData.isNotEmpty()) {
                                    val jsonObject = JSONObject(decryptData)
                                    if (jsonObject.optBoolean("status")) {
                                        val jsonObject1 = jsonObject.getJSONObject("data")
                                        PlayerHelper(
                                            jsonObject1.getString("hls_url"),
                                            context,
                                            jsonObject1.toString(),
                                            initializeMyAppPlayer
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "" + jsonObject.optString("message"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Response is Blank Please Try Again",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            } catch (e: IOException) {
                                e.printStackTrace()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                            Toast.makeText(context, "" + t.message.toString(), Toast.LENGTH_SHORT)
                                .show()

                        }
                    })

            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    class PlayerHelper(
        val itemUrl: String?,
        val _mActivity: Context?,
        val actualtoken: String,
        val initializePlayerService: InitializeMyAppPlayer?
    ) {
        init {
            initializePlayer()
        }

        private fun initializePlayer() {
            try {
                initializePlayerService!!.initializePlayer(itemUrl, actualtoken)
            } catch (e: Exception) {
                if (e is NoSuchMethodException) {
                    Toast.makeText(_mActivity, e.message, Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
                return
            }
        }

        companion object {
            private const val TAG = "PlayerHelper"
        }
    }


}
