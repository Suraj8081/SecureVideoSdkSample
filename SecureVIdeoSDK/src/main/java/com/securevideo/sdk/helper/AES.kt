package com.securevideo.sdk.helper

import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object AES {
    private const val CIPHER_NAME = "AES/CBC/NoPadding"
    private const val CIPHER_KEY_LEN = 16


    private  fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }
    @JvmStatic
    fun encrypt(data: String,accesskey: String): String {
        return try {
            val md5Value= md5(accesskey)
            val key = md5Value.substring(0,16)
            val iv = md5Value.substring(16,32)
            val ivSpec = IvParameterSpec(iv.toByteArray(charset("UTF-8")))
            val secretKey = SecretKeySpec(fixKey(key).toByteArray(charset("UTF-8")), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedData = cipher.doFinal(data.toByteArray(charset("UTF-8")))
            val encryptedDataInBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT)
            "$encryptedDataInBase64:"
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }
    private fun fixKey(key: String): String {
        var data = key
        if (data.length < CIPHER_KEY_LEN) {
            val numPad = CIPHER_KEY_LEN - key.length
            for (i in 0 until numPad) {
                data += "0" //0 pad to len 16 bytes
            }
            return data
        }
        return if (data.length > CIPHER_KEY_LEN) {
            data.substring(0, CIPHER_KEY_LEN) //truncate to 16 bytes
        } else data
    }
    @JvmStatic
    fun decrypt(data: String,accesskey: String): String {
        val md5Value= md5(accesskey)
        return try {
            val key = md5Value.substring(0,16)
            val ivParameter = md5Value.substring(16,32)
            if (data.contains(":")) {
                val parts = data.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val iv = IvParameterSpec(ivParameter.toByteArray())
                val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "AES")
                val cipher = Cipher.getInstance(CIPHER_NAME)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
                val decodedEncryptedData = Base64.decode(parts[0], Base64.NO_PADDING)
                val original = cipher.doFinal(decodedEncryptedData)
                String(original)
            } else {
                val iv = IvParameterSpec(ivParameter.toByteArray())
                val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "AES")
                val cipher = Cipher.getInstance(CIPHER_NAME)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
                val decodedEncryptedData = Base64.decode(data, Base64.NO_PADDING)
                val original = cipher.doFinal(decodedEncryptedData)
                String(original)
            }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }
}
