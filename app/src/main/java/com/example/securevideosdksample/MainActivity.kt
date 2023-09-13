package com.example.securevideosdksample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.example.securevideosdksample.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!


    companion object {
        val fileNme = "Encrypted.mp4"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVod.setOnClickListener {
            if (binding.etUrl.text.isNotEmpty()) {
                val bundle = Bundle()
                bundle.apply {
                    putString("mediaId", binding.etUrl.text.toString())
                    putBoolean("videoType", false)
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            }
        }

        binding.btnLive.setOnClickListener {
            if (binding.etUrl.text.isNotEmpty()) {
                val bundle = Bundle()
                bundle.apply {
                    putString("mediaId", binding.etUrl.text.toString())
                    putBoolean("videoType", true)
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            }

        }

        binding.btnOffline.setOnClickListener {

            if (File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/.Videos/" + fileNme).exists()) {
                val bundle = Bundle()
                bundle.apply {
                    putBoolean("videoType", false)
                    putString("password", "6149730")
                    putBoolean("offline", true)
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            } else {
                Toast.makeText(this, "No Download Video Found", Toast.LENGTH_SHORT).show()
            }


        }

    }


}