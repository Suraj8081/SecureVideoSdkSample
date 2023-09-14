package com.example.securevideosdksample.download.activity


import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.securevideosdksample.VideoPlayer
import com.example.securevideosdksample.databinding.FragmentDownalodedVideoBinding
import com.example.securevideosdksample.downloadService.DownloadService
import com.example.securevideosdksample.model.UrlResponse
import com.example.securevideosdksample.showToast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.securevideosdksample.download.adapter.DownloadAdapter
import com.example.securevideosdksample.download.interfaces.OnItemClick
import com.example.securevideosdksample.room.CareerwillDatabase
import com.example.securevideosdksample.room.table.DownloadVideoTable
import com.example.securevideosdksample.room.viewModel.DownloadVideoModel
import com.example.securevideosdksample.room.viewModel.MyViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DownloadedVideoActivity : AppCompatActivity(), OnItemClick {
    var downloadList = mutableListOf<UrlResponse>()


    companion object {
        const val DOWNLOAD_RUNNING = "Downloading Running"
        const val DOWNLOAD_PAUSE = "Downloading Pause"
        const val DOWNLOADED = "Downloaded"
        const val CANCEL = "CANCEL"
        const val PAUSE = "PAUSE"
        const val RESUME = "RESUME"
        private const val REQUEST_PERMISSION_PHONE_STATE = 1

    }

    lateinit var binding: FragmentDownalodedVideoBinding

    private lateinit var downloadVideoModel: DownloadVideoModel
    lateinit var downloadAdapter: DownloadAdapter
    var userId = ""
    lateinit var downloadVideoList: List<DownloadVideoTable>
    var courseId = ""
    private var videoId = ""
    private var mediaId = ""


    private var careerwillDatabase: CareerwillDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentDownalodedVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.extras?.let {
            videoId = it.getString("videoId", "")
            mediaId = it.getString("mediaId", "")
            courseId = it.getString("courseId", "")
            userId = it.getString("userId", "")
            val urlResponse = it.getString("downloadList", "")
            if (urlResponse.isNotEmpty()) {
                downloadList =
                    Gson().fromJsonList<UrlResponse>(urlResponse.toString()).toMutableList()
            }
        }
        careerwillDatabase = CareerwillDatabase.getInstance(this)
        val factory = MyViewModelFactory(careerwillDatabase)
        downloadVideoModel = ViewModelProvider(this, factory)[DownloadVideoModel::class.java]
        downloadAdapter = DownloadAdapter(this, this, userId)
        binding.rvVideos.adapter = downloadAdapter
        downloadVideoModel.getAllUserData(userId, courseId)
        downloadVideoModel.data.observe(this) {
            it?.let {
                downloadVideoModel.removeObserverData()
                downloadVideoList = it
                downloadAdapter.submitList(it)
            }
        }



        binding.download.setOnClickListener {
            downloadVideoDialog(downloadList)
        }


        notficationPermisson()

    }

    private fun notficationPermisson() {
        if (Build.VERSION.SDK_INT > 32) {
            val permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    requestPermission(
                        Manifest.permission.POST_NOTIFICATIONS, REQUEST_PERMISSION_PHONE_STATE
                    )

                } else {
                    requestPermission(
                        Manifest.permission.POST_NOTIFICATIONS, REQUEST_PERMISSION_PHONE_STATE
                    )
                }
            }
        }

    }

    private fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_PHONE_STATE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@DownloadedVideoActivity, "Permission Granted!", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@DownloadedVideoActivity,
                    "Permission Need For Notidcation!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private inline fun <reified T> Gson.fromJsonList(json: String): List<T> =
        fromJson(json, object : TypeToken<List<T>>() {}.type)


    private fun downloadVideoDialog(link: List<UrlResponse>?) {
        var alertPosition = -1
        link?.map { it.label }?.toTypedArray()?.let {
            val alertDialog = AlertDialog.Builder(this@DownloadedVideoActivity)
            alertDialog.setTitle("Download Video")
            alertDialog.setSingleChoiceItems(it, (alertPosition)) { _, which ->
                alertPosition = which
            }

            alertDialog.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
                dialog?.dismiss()
                dialog?.cancel()
                beginDownload(downloadList[alertPosition])


            }

            alertDialog.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                dialog.cancel()
            }

            alertDialog.setOnCancelListener { it ->
                it.cancel()
                it.dismiss()
            }

            val alert = alertDialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()
        }
    }

    private fun beginDownload(urlResponse: UrlResponse) {
        val videoname = urlResponse.url.substring(urlResponse.url.lastIndexOf('/') + 1)
        val fileName = "${videoId}_${userId}_${courseId}"
        val downloadTable = DownloadVideoTable(
            name = videoname,
            videoId = videoId,
            videoUrl = urlResponse.url,
            token = urlResponse.meta.password,
            videoStatus = "Downloading Running",
            thumbnail_url = "",
            userId = userId,
            fileName = fileName,
            courseId = courseId
        )
        careerwillDatabase?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = db.downloadDao().isRecordExist(videoId, userId, courseId)
                if (!result) {
                    db.downloadDao().insert(downloadTable).let {
                        withContext(Dispatchers.Main) {
                            downloadVideoModel.getAllUserData(userId, courseId)

                            val videoDownloadIntent =
                                Intent(this@DownloadedVideoActivity, DownloadService::class.java)
                            videoDownloadIntent.apply {
                                putExtra(DownloadService.VIDEONAME, videoname)
                                putExtra(DownloadService.DOWNLOAD_SERVICE_ID, videoId)
                                putExtra(DownloadService.URL, urlResponse.url)
                                putExtra("userId", userId)
                                putExtra("filePath", "${videoId}_${userId}_${courseId}")
                                putExtra("status", "Downloading Running")
                                putExtra("course_id", courseId)
                                putExtra(DownloadService.FILEDOWNLOADSTATUS, false)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(videoDownloadIntent)
                            } else {
                                startService(videoDownloadIntent)
                            }
                        }
                    }

                } else {
                    db.downloadDao().getVideoData(videoId, userId, courseId)?.let {
                        withContext(Dispatchers.Main) {
                            when (it.videoStatus) {
                                DOWNLOAD_RUNNING -> {
                                    showToast("Video is Downloading Please Wait")
                                }

                                DOWNLOAD_PAUSE -> {
                                    showToast("Video is Paused")
                                }

                                DOWNLOADED -> {
                                    showToast("Video is  Already Downloaded")
                                }
                            }
                        }
                    }

                }
            }
        }

    }


    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            videoDownloadReceiver, IntentFilter(DownloadService.VIDEO_DOWNLOAD_ACTION)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            percentageReceiver, IntentFilter(DownloadService.VIDEO_DOWNLOAD_PROGRESS)
        )

    }


    override fun OnVideoClick(pos: Int, videoDownload: DownloadVideoTable?, type: String?) {
        when (type) {
            CANCEL -> {
                deleteVideo(pos, videoDownload);
            }

            PAUSE -> {
                videoDownload?.let {
                    pauseVideo(it.videoId, type)

                }
            }

            RESUME -> {
                videoDownload?.let {
                    resumeDownloadVideo(pos, it.videoId, type)

                }
            }
        }
    }

    private fun resumeDownloadVideo(pos: Int, videoId: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            careerwillDatabase?.downloadDao()?.getVideoData(videoId, userId, courseId)?.let {
                withContext(Dispatchers.Main) {
                    resumeVideo(it)
                }
            }
        }
    }


    private fun deleteVideo(pos: Int, videoDownload: DownloadVideoTable?) {
        videoDownload?.apply {
            when (videoDownload.videoStatus) {
                DOWNLOAD_RUNNING, DOWNLOAD_PAUSE, DOWNLOADED -> {
                    val file =
                        File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADED_VIDEOS + videoDownload.fileName + ".mp4")
                    val fileProcessing =
                        File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADING_VIDEOS + videoDownload.fileName + ".mp4")
                    when (videoDownload.percentage) {
                        100 -> {
                            if (fileProcessing.exists()) {
                                fileProcessing.delete()
                            }
                            if (file.exists()) {
                                file.delete()
                            }
                            deleteVideoFromDB(pos, videoDownload.videoId)

                        }

                        else -> {
                            when (videoDownload.videoStatus) {
                                DOWNLOAD_PAUSE -> {
                                    if (fileProcessing.exists()) {
                                        fileProcessing.delete()
                                    }

                                    if (file.exists()) {
                                        file.delete()
                                    }
                                    deleteVideoFromDB(pos, videoDownload.videoId)
                                }

                                else -> {
                                    if (DownloadService.videoId == videoDownload.videoId) {
                                        try {
                                            DownloadService.action = DownloadService.CANCEL
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        if (fileProcessing.exists()) {
                                            fileProcessing.delete()
                                        }
                                        if (file.exists()) {
                                            file.delete()
                                        }
                                        deleteVideoFromDB(pos, videoDownload.videoId)


                                    }
                                }
                            }
                        }

                    }
                }
            }

        }
    }

    private fun pauseVideo(videoId: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            careerwillDatabase?.downloadDao()?.getVideoData(videoId, userId, courseId)?.let {
                withContext(Dispatchers.Main) {
                    it.let {
                        if ((DownloadService.videoId == it.videoId) && it.percentage > 0) {
                            DownloadService.action = DownloadService.PAUSE
                        }
                    }
                }
            }
        }

    }

    private fun resumeVideo(adapterData: DownloadVideoTable) {
        try {
            var videoDownloadIntent: Intent? = null
            videoDownloadIntent = Intent(this, DownloadService::class.java)
            when (adapterData.totalDownloadLocale) {
                0L -> {
                    adapterData.percentage = 0
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                careerwillDatabase?.downloadDao()?.updateVideoStatus(
                    adapterData.videoId,
                    "0",
                    "Downloading Running",
                    adapterData.percentage,
                    userId,
                    courseId
                )?.let {
                    withContext(Dispatchers.Main) {
                        if (it > -1) {
                            val cpyList = downloadVideoList.toMutableList()
                            cpyList.indices.find { it -> cpyList[it].videoId == adapterData.videoId }
                                ?.let { index ->
                                    val data = cpyList[index].copy()
                                    data.videoStatus = "Downloading Running"
                                    data.percentage = adapterData.percentage
                                    cpyList[index] = data
                                    downloadAdapter.submitList(cpyList)
                                    downloadVideoList = cpyList

                                    videoDownloadIntent.apply {
                                        putExtra(DownloadService.VIDEONAME, adapterData.name)
                                        putExtra(
                                            DownloadService.DOWNLOAD_SERVICE_ID, adapterData.videoId
                                        )
                                        putExtra(DownloadService.URL, adapterData.videoUrl)
                                        putExtra("userId", userId)
                                        putExtra(
                                            "filePath",
                                            "${adapterData.videoId}_${userId}_${courseId}"
                                        )
                                        putExtra("status", "Downloading Running")
                                        putExtra("course_id", courseId)
                                        putExtra(
                                            DownloadService.FILEDOWNLOADSTATUS,
                                            adapterData.percentage > 0
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            startForegroundService(videoDownloadIntent)
                                        } else {
                                            startService(videoDownloadIntent)
                                        }
                                    }
                                }
                        }

                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun deleteVideoFromDB(pos: Int, videoId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            careerwillDatabase?.downloadDao()?.deleteVideoSuspend(videoId, userId, courseId)
                ?.let { status ->
                    withContext(Dispatchers.Main) {
                        if (status > -1) {
                            val list = downloadVideoList.toMutableList()
                            if (list.isNotEmpty()) {
                                list.removeAt(pos)
                                downloadAdapter.submitList(list)
                                downloadVideoList = list
                            } else {
                                downloadVideoModel.getAllUserData(userId, courseId)
                            }

                        }
                    }

                }
        }

    }

    private val videoDownloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val serviceStatus = intent.getIntExtra("result", -1)
            val videoId = intent.getStringExtra(DownloadService.VIDEOID)
            when (serviceStatus) {
                DownloadService.VIDEO_DOWNLOAD_PAUSED, DownloadService.VIDEO_DOWNLOAD_SUCCESSFUL -> {
                    videoId?.let {
                        completeReceiver(videoId)
                    }
                }

                DownloadService.VIDEO_FILE_EXIST -> {
                    showToast("Retry Downalod")
                }

                DownloadService.VIDEO_DOWNLOAD_CANCELLED, 2021 -> {
                    downloadVideoModel.getAllUserData(userId, courseId)
                }

                DownloadService.EXCEPTION_OCCURRED -> {
                    showToast("No internet connection")
                    downloadVideoModel.getAllUserData(userId, courseId)

                }

                DownloadService.NOT_AVAILABLE_ON_SERVER -> {
                    showToast("Server issue please retry again")
                    downloadVideoModel.getAllUserData(userId, courseId)
                }

                DownloadService.VIDEO_DOWNLOAD_STARTED -> {
                    if (downloadVideoList.isEmpty()) {
                        videoId?.let {
                            videoDownloadStarted(videoId)
                        }
                    }

                }
            }
        }
    }

    private fun completeReceiver(videoId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            careerwillDatabase?.downloadDao()?.getVideoData(videoId, userId, courseId)
                ?.let { videoDownloadData ->
                    videoDownloadData.videoId.let {
                        val cpyList = downloadVideoList.toMutableList()
                        cpyList.indices.find { it -> cpyList[it].videoId == videoId }
                            ?.let { index ->
                                cpyList[index] = videoDownloadData
                                downloadAdapter.submitList(cpyList)
                                downloadVideoList = cpyList
                            }
                    }
                }

        }

    }


    private fun videoDownloadStarted(videoId: String) {
        if (downloadVideoList.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                careerwillDatabase?.downloadDao()
                    ?.getVideoIsComplete(videoId, userId, "0", courseId)?.let { videoDownloadData ->
                        videoDownloadData.videoId.let {
                            val cpyList = downloadVideoList.toMutableList()
                            cpyList.indices.find { it -> cpyList[it].videoId == videoId }
                                ?.let { index ->
                                    cpyList[index] = videoDownloadData
                                    downloadAdapter.submitList(cpyList)
                                    downloadVideoList = cpyList
                                }
                        }
                    }

            }

        }

    }

    private val percentageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val videoId = intent.getStringExtra(DownloadService.VIDEOID)
            videoId?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    careerwillDatabase?.downloadDao()?.getVideoIsComplete(it, userId, "0", courseId)
                        ?.let { data ->
                            data.videoId.let {
                                val cpyList = downloadVideoList.toMutableList()
                                cpyList.indices.find { it -> cpyList[it].videoId == videoId }
                                    ?.let { index ->
                                        withContext(Dispatchers.Main) {
                                            cpyList[index] = data
                                            downloadAdapter.submitList(cpyList)
                                            downloadVideoList = cpyList
                                        }

                                    }
                            }
                        }
                }
            }


        }
    }


    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(videoDownloadReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(percentageReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun clickOnDownloadVide(downloadVideoTable: DownloadVideoTable) {
        if (downloadVideoTable.percentage >= 100) {
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADED_VIDEOS + downloadVideoTable.fileName + ".mp4")
            val fileProcessing =
                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADING_VIDEOS + downloadVideoTable.fileName + ".mp4")

            if (file.exists()) {
                val bundle = Bundle()
                bundle.apply {
                    putBoolean("videoType", false)
                    putString("password", downloadVideoTable.token)
                    putBoolean("offline", true)
                    putString("filePath", file.absolutePath.toString())
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            } else if (fileProcessing.exists()) {
                val bundle = Bundle()
                bundle.apply {
                    putBoolean("videoType", false)
                    putString("password", downloadVideoTable.token)
                    putBoolean("offline", true)
                    putString("filePath", fileProcessing.absolutePath.toString())
                }
                Intent(this, VideoPlayer::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
            }
        }
    }


}