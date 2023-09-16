package com.example.securevideosdksample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.example.securevideosdksample.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

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


    }

    override fun onDestroy() {
        super.onDestroy()
        binding =null
    }
}