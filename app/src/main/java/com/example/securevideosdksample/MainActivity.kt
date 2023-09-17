package com.example.securevideosdksample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.securevideosdksample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    companion object{

        private const val REQUEST_PERMISSION_PHONE_STATE = 1

    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
//        binding!!.etUrl.setText("60075ff276")
        binding!!.btnVod.setOnClickListener {
            if (binding!!.etUrl.text.isNotEmpty()) {
                val bundle = Bundle()
                bundle.apply {
                    putString("mediaId", binding!!.etUrl.text.toString())
                    putString("videoId",binding!!.etUrl.text.toString().substring(0,2))
                    putBoolean("videoType", false)
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            }
        }

        binding!!.btnLive.setOnClickListener {
            if (binding!!.etUrl.text.isNotEmpty()) {
                val bundle = Bundle()
                bundle.apply {
                    putString("mediaId", binding!!.etUrl.text.toString())
                    putBoolean("videoType", true)
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            }

        }

        notficationPermisson()


    }

    private fun notficationPermisson() {
        if (Build.VERSION.SDK_INT > 32) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    requestPermission(
                        Manifest.permission.POST_NOTIFICATIONS,
                        REQUEST_PERMISSION_PHONE_STATE
                    )

                } else {
                    requestPermission(
                        Manifest.permission.POST_NOTIFICATIONS,
                        REQUEST_PERMISSION_PHONE_STATE
                    )
                }
            }
        }

    }
    private fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), permissionRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_PHONE_STATE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Permission Need For Notidcation!", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding =null
    }
}