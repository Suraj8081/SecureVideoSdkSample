package com.securevideo.sdk.helper

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.util.Base64
import com.securevideo.sdk.helper.ServiceWithOutDrm.Companion.baseUrl
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.URLEncoder
import java.nio.channels.FileChannel
import java.util.UUID


class VideoPlayerInit(val context: Context) {
    var service: ServiceWithOutDrm? = null


    private fun getBase64(input: String): String? {
        return Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
    }
    fun initialize(initializeMyAppPlayer: InitializeMyAppPlayer?, userId: String, siteid: String, accessKey:String, secretKey: String, medidaId: String, videoType:Boolean): ServiceWithOutDrm? {
        val encoded = "${getBase64( "$accessKey:$secretKey")}"
        service = ServiceWithOutDrm(context, initializeMyAppPlayer, userId, siteid, "Bearer $encoded", medidaId,accessKey,videoType)
        return service
    }
    private fun deviceDetails(uuid: UUID): String {
        return JSONObject().apply {
            put("Build.PRODUCT", Build.PRODUCT)
            put("Build.MANUFACTURER", Build.MANUFACTURER)
            put("Build.BRAND", Build.BRAND)
            put("Build.DEVICE", Build.DEVICE)
            put("Build.MODEL", Build.MODEL)
            put("Build.HARDWARE", Build.HARDWARE)
            put("Build.FINGERPRINT", Build.FINGERPRINT)
            widevineModularDrmInfo(uuid)?.let {
                put("DRM",it.toString())
            }
        }.toString()

    }

    @Throws(JSONException::class)
    private fun widevineModularDrmInfo(uuid: UUID): JSONObject? {
        if (!MediaDrm.isCryptoSchemeSupported(uuid)) {
            return null
        }
        try {
            val mediaDrm = MediaDrm(uuid)
            val stringProps = arrayOf(
                MediaDrm.PROPERTY_VENDOR,
                MediaDrm.PROPERTY_VERSION,
                MediaDrm.PROPERTY_DESCRIPTION,
                MediaDrm.PROPERTY_ALGORITHMS,
                "securityLevel",
                "systemId",
                "privacyMode",
                "sessionSharing",
                "usageReportingSupport",
                "appId",
                "origin",
                "hdcpLevel",
                "maxHdcpLevel",
                "maxNumberOfSessions",
                "numberOfOpenSessions"
            )
            val props = JSONObject()
            for (prop in stringProps) {
                val value: String = try {
                    mediaDrm.getPropertyString(prop)
                } catch (e: java.lang.IllegalStateException) {
                    "exception"
                }
                props.put(prop, value)
            }
            val response = JSONObject()
            return  response.put("properties",props)
        }
        catch (e: java.lang.Exception) {
            return null
        }
    }


    fun prePareLicense(siteId: String, accessKey: String, mediaId: String, userId: String,ttlSecond:Int,uuid: UUID):String?
    {
        val paramsData = JSONObject().apply {
            put("site_id",siteId)
            put("access_key",accessKey)
            put("media_id",mediaId)
            put("user_id",userId)
            put("timestamp",System.currentTimeMillis()+(ttlSecond*1000))
            put("device_type","Android")
            put("version","1")
            put("deviceDetails",deviceDetails(uuid))
        }.toString()

        return "${baseUrl}get-drm-licence?siteid=${siteId}&tk=${URLEncoder.encode(AES.encrypt(paramsData,accessKey), "UTF-8")}"
    }



    companion object {
        private var instance: VideoPlayerInit? = null
        fun getInstance(context: Context): VideoPlayerInit? {
            if (instance == null) instance = VideoPlayerInit(context)
            return instance
        }
    }

    fun encrypt(strFile: String, key: String): Boolean {
        var result= false
        val LENGTH = 28
        var len = LENGTH
        try {
            val f = File(strFile)
            if (f.exists()) {
                val raf = RandomAccessFile(f, "rw")
                val totalLen = raf.length()
                if (totalLen < LENGTH)
                    len = totalLen.toInt()
                val channel = raf.channel
                val buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, LENGTH.toLong())
                var tmp: Byte
                for (i in 0 until len) {
                    val rawByte = buffer[i]
                    tmp = if (i <= key.length - 1) {
                        (rawByte.toInt() xor key[i].code).toByte()
                    } else {
                        (rawByte.toInt() xor i).toByte()
                    }
                    buffer.put(i, tmp)
                }
                buffer.force()
                buffer.clear()
                channel.close()
                raf.close()
                result= true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return result
    }

}