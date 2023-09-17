package com.example.securevideosdksample

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.securevideosdksample.databinding.VideoPlayerBinding
import com.example.securevideosdksample.download.activity.DownloadedVideoActivity
import com.example.securevideosdksample.downloadService.DownloadService
import com.example.securevideosdksample.model.UrlResponse
import com.example.securevideosdksample.room.CareerwillDatabase
import com.example.securevideosdksample.room.table.DownloadVideoTable
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.DrmConfiguration
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.securevideo.sdk.helper.InitializeMyAppPlayer
import com.securevideo.sdk.helper.VideoPlayerInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy


class VideoPlayer : AppCompatActivity(), InitializeMyAppPlayer {

    private var player: ExoPlayer? = null
    private var mExoPlayerFullscreen = false


    private lateinit var trackSelectorParameters: DefaultTrackSelector.Parameters
    private var trackSelector: DefaultTrackSelector? = null


    private var _binding: VideoPlayerBinding? = null
    private val binding get() = _binding!!

    private var selectedQualityIndex = 0
    private var downloadPos = -1

    private var shouldAutoPlay = false


    private var resumeWindow = 0
    private var resumePosition: Long = 0
    private var mFullScreenIcon: ImageView? = null

    private var cookieValue = ""
    private var is_live = ""

    private var downloadID: Long = 0

    private var downloadFilePath: File? = null

    val downloadList = mutableListOf<UrlResponse>()
    val onlinePlayList = mutableListOf<UrlResponse>()

    val siteId = "XX"
    val userId = "XX"
    private val courseId = "XXX"
    val accesskey = "XXXXXXXXXXXXXXXXXXXXXX"
    val secretKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"

    /////////recorder
    var mediaId = ""
    private var videoId = ""
    private var videoType = false
    private var offline = false
    private var downloadVideoData: DownloadVideoTable? = null

    private var liveurl: String = ""
    private var vodUrl: String = ""
    private var password: String = ""


    companion object {
        private var DEFAULT_COOKIE_MANAGER: CookieManager? = null

        init {
            DEFAULT_COOKIE_MANAGER = CookieManager()
            DEFAULT_COOKIE_MANAGER!!.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

    }


    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            if (downloadFilePath != null) {
                VideoPlayerInit.getInstance(this@VideoPlayer)
                    ?.encrypt(downloadFilePath!!.absolutePath, password)
                val videoURL = downloadFilePath!!.absoluteFile.toString()
                shouldAutoPlay = true
                val mediaSource = buildMediaSource(Uri.parse(videoURL), applicationContext)
                getPlayer(mediaSource)

            } else if (liveurl.isNotEmpty()) {
                val mediaSource = buildMediaSource(Uri.parse(liveurl), this)
                getPlayer(mediaSource)
            } else if (vodUrl.isNotEmpty()) {
                val licenceUrl = VideoPlayerInit.getInstance(this@VideoPlayer)
                    ?.prePareLicense(siteId, accesskey, mediaId, userId, 30, C.WIDEVINE_UUID)
                licenceUrl?.let { it1 -> playDrmVideo(vodUrl, it1) }
            }

            ////////////video Download/////////
            LocalBroadcastManager.getInstance(this).registerReceiver(
                videoDownloadReceiver, IntentFilter(DownloadService.VIDEO_DOWNLOAD_ACTION)
            )
            LocalBroadcastManager.getInstance(this).registerReceiver(
                percentageReceiver, IntentFilter(DownloadService.VIDEO_DOWNLOAD_PROGRESS)
            )

        }
    }

    private fun completeReceiver(videoId: String) {
        CareerwillDatabase.getInstance(this)?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.downloadDao().getVideoData(videoId, userId, courseId)?.let { data ->
                        withContext(Dispatchers.Main) {
                            if (data.percentage == 100) {
                                binding.playVideoDownloaded.isVisible = true
                                binding.videoDownload.isVisible = false
                                binding.deleteVideo.isVisible = true
                                binding.progressCvr.visibility = View.GONE
                                binding.parentLayout.visibility = View.GONE
                                getVideoOfflineData()

                            } else {
                                binding.videoTitle.text = data.name
                                binding.pauseBtn.isVisible = true

                                data.videoStatus.let {
                                    when (it) {
                                        "Downloading Pause" -> {
                                            binding.pauseBtn.setImageDrawable(
                                                ContextCompat.getDrawable(
                                                    this@VideoPlayer, R.drawable.play_button
                                                )
                                            )
                                        }

                                        "Downloading Running" -> {
                                            binding.pauseBtn.setImageDrawable(
                                                ContextCompat.getDrawable(
                                                    this@VideoPlayer,
                                                    R.drawable.ic_video_download_pause
                                                )
                                            )
                                        }

                                    }

                                }

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
                    binding.videoTitle.text = ""
                    binding.fileMb.text = ""
                    binding.progressValue.progress = 0
                    binding.percentageValue.text = ""

                    binding.pauseBtn.isVisible = false
                    binding.parentLayout.isVisible = false
//                    downloadVideoModel.getAllUserData(userId,courseId)
                }

                DownloadService.EXCEPTION_OCCURRED -> {
                    showToast("No internet connection")
//                    downloadVideoModel.getAllUserData(userId,courseId)

                }

                DownloadService.NOT_AVAILABLE_ON_SERVER -> {
                    showToast("Server issue please retry again")
//                    downloadVideoModel.getAllUserData(userId,courseId)
                }

                DownloadService.VIDEO_DOWNLOAD_STARTED -> {
                    if (!binding.parentLayout.isVisible) {
                        videoId?.let {
                            binding.parentLayout.isVisible = true
                            videoDownloadStarted(videoId)
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
                CareerwillDatabase.getInstance(this@VideoPlayer)?.let { db ->
                    CoroutineScope(Dispatchers.IO).launch {
                        db.downloadDao().getVideoIsComplete(it, userId, "0", courseId)
                            ?.let { data ->
                                withContext(Dispatchers.Main) {
                                    binding.fileMb.text = data.lengthInMb
                                    if (data.percentage == 100) {
                                        binding.deleteVideo.isVisible = true
                                        binding.progressCvr.visibility = View.GONE
                                    } else {
                                        binding.videoTitle.text = data.name
                                        binding.pauseBtn.isVisible = true


                                        binding.progressValue.progress = data.percentage
                                        binding.percentageValue.text = "${data.percentage} % Done"
                                        data.videoStatus.let {
                                            when (it) {
                                                "Downloading Pause" -> {
                                                    binding.pauseBtn.setImageDrawable(
                                                        ContextCompat.getDrawable(
                                                            context, R.drawable.play_button
                                                        )
                                                    )
                                                }

                                                "Downloading Running" -> {
                                                    binding.pauseBtn.setImageDrawable(
                                                        ContextCompat.getDrawable(
                                                            context,
                                                            R.drawable.ic_video_download_pause
                                                        )
                                                    )
                                                }

                                            }

                                        }
                                    }

                                }
                            }
                    }

                }
            }


        }
    }


    private fun videoDownloadStarted(videoId: String) {
        CareerwillDatabase.getInstance(this)?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.downloadDao().getVideoIsComplete(videoId, userId, "0", courseId)
                    ?.let { videoDownloadData ->
                        withContext(Dispatchers.Main) {
                            binding.videoTitle.text = videoDownloadData.name
                            binding.pauseBtn.isVisible = true
                        }
                    }
            }
        }

    }


    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(videoDownloadReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(percentageReceiver)

        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }

        downloadFilePath?.let {
            VideoPlayerInit.getInstance(this@VideoPlayer)
                ?.encrypt(downloadFilePath!!.absolutePath, password)

        }

    }


    private fun releasePlayer() {
        shouldAutoPlay = player?.playWhenReady ?: false
        updateResumePosition()
        playerEventListener.let {
            player?.removeListener(playerEventListener)
        }
        player?.release()
        player = null
        trackSelector = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = VideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.extras?.let {
            mediaId = it.getString("mediaId", "613a3e6433")
            videoType = it.getBoolean("videoType", false)
            offline = it.getBoolean("offline", false)
            password = it.getString("password", "")
            videoId = it.getString("videoId", "")
        }


        shouldAutoPlay = true
        clearResumePosition()


        ////////videoType - true - live
        ////////videoType - false -vod
        binding.download.isVisible = !videoType

        if (offline) {
            downloadFilePath = intent?.extras?.getString("filePath", "")?.let { File(it) }
            binding.quality.isVisible = false
            binding.download.isVisible = false
        } else {
            if (!videoType) {
                binding.videoDownload.isVisible = true
                CareerwillDatabase.getInstance(this)?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        it.downloadDao().getVideoData(videoId, userId, courseId)?.let {
                            withContext(Dispatchers.Main) {
                                offlinePlay(it)
                            }
                        }
                    }
                }
            }
            VideoPlayerInit.getInstance(this)
                ?.initialize(this, userId, siteId, accesskey, secretKey, mediaId, videoType)
        }

        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

        binding.quality.setOnClickListener {
            trackSelector?.currentMappedTrackInfo?.let {
                if (onlinePlayList.size > 0) {
                    alertDialog(onlinePlayList)
                } else {
                    trackSelector?.currentMappedTrackInfo?.let {
                        val trackSelectionDialog: TrackSelectionDialog =
                            TrackSelectionDialog.createForTrackSelector(trackSelector!!) { false }
                        trackSelectionDialog.show(supportFragmentManager, null)
                    }
                }
            }
        }

        val controlView =
            binding.exoPlayer.findViewById<PlayerControlView>(com.google.android.exoplayer2.ui.R.id.exo_controller)
        val exoForward = controlView.findViewById<ImageView>(R.id.exo_ffwd_new)
        val exoRew = controlView.findViewById<ImageView>(R.id.exo_rew_new)
        val fullScreen = controlView.findViewById<FrameLayout>(R.id.exo_fullscreen_button)
        val goLive = controlView.findViewById<TextView>(R.id.tv_go_live)
        val speed = controlView.findViewById<ImageView>(R.id.speed)
        mFullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon)

        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val scale = resources.displayMetrics.density
        var pixels = (220 * scale + 0.5f).toInt()
        val xlarge =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == 4
        val large =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE
        pixels = if (large) {
            (350 * scale + 0.5f).toInt()
        } else if (xlarge) {
            (450 * scale + 0.5f).toInt()
        } else {
            (230 * scale + 0.5f).toInt()
        }
        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pixels)
        binding.rootNew.layoutParams = layoutParams


        goLive.setOnClickListener {
            trackSelector?.currentMappedTrackInfo?.let {
                player?.seekTo(player?.duration ?: 0)

            }
        }
        speed.setOnClickListener { view ->
            trackSelector?.currentMappedTrackInfo?.let {
                showSpeedOptions(view)
            }
        }

        exoForward.setOnClickListener {
            var currentPos = 0L
            var duration = 0L
            player?.currentPosition?.let { currentPos = it }
            player?.duration?.let { duration = it }

            if (currentPos < (duration - 10000)) player?.seekTo(currentPos.plus(10000))
        }
        exoRew.setOnClickListener {
            var currentPos = 0L
            player?.currentPosition?.let { currentPos = it }

            if (currentPos > 10000) player!!.seekTo(player!!.currentPosition - 10000)

        }
        fullScreen.setOnClickListener {


            if (!mExoPlayerFullscreen) {
                landscapePlayer()
            } else {
                portraitPlayer()
            }
        }


        binding.download.setOnClickListener {
            if (downloadList.size > 0) {
                pausePlayer()
                val intent = Intent(this, DownloadedVideoActivity::class.java)
                val bundle = Bundle()
                bundle.putString("downloadList", Gson().toJson(downloadList))
                ////////////example video like
                bundle.putString("videoId", videoId)
                bundle.putString("mediaId", mediaId)
                bundle.putString("userId", userId)
                bundle.putString("courseId", courseId)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            }

        }

        binding.videoDownload.setOnClickListener {
            if (downloadList.size > 0) {
                pausePlayer()
                downloadVideoDialog(downloadList)
            }
        }

        binding.pauseBtn.setOnClickListener {
            CareerwillDatabase.getInstance(this)?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.downloadDao().getVideoData(videoId, userId, courseId)?.let { data ->
                        withContext(Dispatchers.Main) {
                            data.videoStatus.let {
                                when (it) {
                                    "Downloading Running" -> {
                                        onVideoClick(videoDownload = data, type = "PAUSE")
                                    }

                                    "Downloading Pause" -> {
                                        onVideoClick(videoDownload = data, type = "RESUME")
                                    }

                                }

                            }

                        }
                    }
                }
            }
        }
        binding.cancelBtn.setOnClickListener {

            android.app.AlertDialog.Builder(this).setTitle("Delete entry")
                .setMessage("Are You Sure You Want to Delete Video ?")
                .setPositiveButton("yes") { _, _ ->
                    CareerwillDatabase.getInstance(this)?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            it.downloadDao().getVideoData(videoId, userId, courseId)?.let { data ->
                                withContext(Dispatchers.Main) {
                                    onVideoClick(videoDownload = data, type = "CANCEL")

                                }
                            }
                        }
                    }

                }.setNegativeButton("No", null).setIcon(android.R.drawable.ic_dialog_alert).show()


        }
        binding.deleteVideo.setOnClickListener {

            android.app.AlertDialog.Builder(this).setTitle("Delete entry")
                .setMessage("Are You Sure You Want to Delete Video ?")
                .setPositiveButton("yes") { _, _ ->
                    CareerwillDatabase.getInstance(this)?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            it.downloadDao().getVideoData(videoId, userId, courseId)?.let { data ->
                                withContext(Dispatchers.Main) {
                                    onVideoClick(videoDownload = data, type = "CANCEL")

                                }
                            }
                        }
                    }

                }.setNegativeButton("No", null).setIcon(android.R.drawable.ic_dialog_alert).show()


        }


    }

    private fun getVideoOfflineData() {
        CareerwillDatabase.getInstance(this)?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.downloadDao().getVideoData(videoId, userId, courseId)?.let {
                    withContext(Dispatchers.Main) {
                        offlinePlay(it)
                    }
                }
            }
        }
    }

    private fun onVideoClick(pos: Int = -1, videoDownload: DownloadVideoTable?, type: String?) {
        when (type) {
            DownloadedVideoActivity.CANCEL -> {
                deleteVideo(pos, videoDownload);
            }

            DownloadedVideoActivity.PAUSE -> {
                videoDownload?.let {
                    pauseVideo(it.videoId, type)

                }
            }

            DownloadedVideoActivity.RESUME -> {
                videoDownload?.let {
                    resumeDownloadVideo(pos, it.videoId, type)

                }
            }
        }
    }

    private fun resumeDownloadVideo(pos: Int, videoId: String, type: String) {
        CareerwillDatabase.getInstance(this)?.let { careerwillDatabase ->
            CoroutineScope(Dispatchers.IO).launch {
                careerwillDatabase?.downloadDao()?.getVideoData(videoId, userId, courseId)?.let {
                    withContext(Dispatchers.Main) {
                        resumeVideo(it)
                    }
                }
            }
        }
    }

    private fun deleteVideo(pos: Int, videoDownload: DownloadVideoTable?) {
        videoDownload?.apply {
            when (videoDownload.videoStatus) {
                DownloadedVideoActivity.DOWNLOAD_RUNNING, DownloadedVideoActivity.DOWNLOAD_PAUSE, DownloadedVideoActivity.DOWNLOADED -> {
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
                                DownloadedVideoActivity.DOWNLOAD_PAUSE -> {
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

    private fun deleteVideoFromDB(pos: Int, videoId: String) {
        CareerwillDatabase.getInstance(this)?.let { careerwillDatabase ->
            CoroutineScope(Dispatchers.IO).launch {
                careerwillDatabase.downloadDao()?.deleteVideoSuspend(videoId, userId, courseId)
                    ?.let { status ->
                        withContext(Dispatchers.Main) {
                            if (status > -1) {
                                binding.videoTitle.text = ""
                                binding.fileMb.text = ""
                                binding.progressValue.progress = 0
                                binding.percentageValue.text = ""
                                binding.pauseBtn.isVisible = false
                                binding.parentLayout.isVisible = false
                                binding.playVideoDownloaded.isVisible = false
                                binding.videoDownload.isVisible = true

                                binding.deleteVideo.isVisible = false
                                binding.progressCvr.visibility = View.VISIBLE

                            }
                        }

                    }
            }
        }
    }


    private fun pauseVideo(videoId: String, type: String) {
        CareerwillDatabase.getInstance(this)?.let { careerwillDatabase ->
            CoroutineScope(Dispatchers.IO).launch {
                careerwillDatabase.downloadDao()?.getVideoData(videoId, userId, courseId)?.let {
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
            CareerwillDatabase.getInstance(this)?.let { careerwillDatabase ->
                CoroutineScope(Dispatchers.IO).launch {
                    careerwillDatabase.downloadDao()?.updateVideoStatus(
                        adapterData.videoId,
                        "0",
                        "Downloading Running",
                        adapterData.percentage,
                        userId,
                        courseId
                    )?.let {
                        withContext(Dispatchers.Main) {
                            if (it > -1) {
                                videoDownloadIntent.apply {
                                    putExtra(DownloadService.VIDEONAME, adapterData.name)
                                    putExtra(
                                        DownloadService.DOWNLOAD_SERVICE_ID, adapterData.videoId
                                    )
                                    putExtra(DownloadService.URL, adapterData.videoUrl)
                                    putExtra("userId", userId)
                                    putExtra(
                                        "filePath", "${adapterData.videoId}_${userId}_${courseId}"
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


    private fun downloadVideoDialog(link: List<UrlResponse>?) {
        var alertPosition = -1
        link?.map { it.label }?.toTypedArray()?.let {
            val alertDialog = AlertDialog.Builder(this@VideoPlayer)
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
        CareerwillDatabase.getInstance(this)?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = db.downloadDao().isRecordExist(videoId, userId, courseId)
                if (!result) {
                    db.downloadDao().insert(downloadTable).let {
                        withContext(Dispatchers.Main) {
                            val videoDownloadIntent =
                                Intent(this@VideoPlayer, DownloadService::class.java)
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
                                DownloadedVideoActivity.DOWNLOAD_RUNNING -> {
                                    showToast("Video is Downloading Please Wait")
                                }

                                DownloadedVideoActivity.DOWNLOAD_PAUSE -> {
                                    showToast("Video is Paused")
                                    resumeVideo(it)
                                }

                                DownloadedVideoActivity.DOWNLOADED -> {
                                    showToast("Video is  Already Downloaded")
                                }
                            }
                        }
                    }
                }
            }


        }

    }


    private fun alertDialog(link: List<UrlResponse>?) {
        var alertPosition = selectedQualityIndex
        link?.map { it.label }?.toTypedArray()?.let {
            val alertDialog = AlertDialog.Builder(this@VideoPlayer)
            alertDialog.setTitle("Quality")
            alertDialog.setSingleChoiceItems(it, (selectedQualityIndex)) { _, which ->
                alertPosition = which
            }

            alertDialog.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
                if (alertPosition != selectedQualityIndex) {
                    selectedQualityIndex = alertPosition
                    dialog?.dismiss()
                    dialog?.cancel()
                    releasePlayer()
                    val licenceUrl = VideoPlayerInit.getInstance(this@VideoPlayer)
                        ?.prePareLicense(siteId, accesskey, mediaId, userId, 30, C.WIDEVINE_UUID)
                    licenceUrl?.let { it1 -> playDrmVideo(link[selectedQualityIndex].url, it1) }
                }


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

    private fun offlinePlay(downloadVideoTable: DownloadVideoTable) {
        val file =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADED_VIDEOS + downloadVideoTable.fileName + ".mp4")
        val fileProcessing =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + DownloadService.DOWNLOADING_VIDEOS + downloadVideoTable.fileName + ".mp4")

        if (downloadVideoTable.percentage >= 100) {
            binding.videoDownload.isVisible = false
            if (file.exists()) {
                downloadVideoData = downloadVideoTable
                binding.playVideoDownloaded.isVisible = true
            } else if (fileProcessing.exists()) {
                downloadVideoData = downloadVideoTable
                binding.playVideoDownloaded.isVisible = true
            }
        } else {
            binding.videoDownload.isVisible = true
            binding.parentLayout.isVisible = true
        }
        binding.playVideoDownloaded.setOnClickListener {
            downloadVideoData?.let {
                if (file.exists()) {
                    restartPlayer(file, it.token)
                } else if (fileProcessing.exists()) {
                    restartPlayer(fileProcessing, it.token)
                }
            }
        }
    }

    private fun restartPlayer(file: File, token: String) {
        pausePlayer()
        releasePlayer()
        binding.quality.isVisible = false
        binding.playVideoDownloaded.isVisible = false
        downloadFilePath = file
        password = token
        VideoPlayerInit.getInstance(this@VideoPlayer)?.encrypt(file.absolutePath, token)
        shouldAutoPlay = true
        getPlayer(buildMediaSource(Uri.parse(file.absolutePath), applicationContext))
    }

    private fun landscapePlayer() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mFullScreenIcon?.setImageDrawable(
            ContextCompat.getDrawable(
                this, R.drawable.fullscreenexit
            )
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val params = binding.rootNew.layoutParams
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.rootNew.layoutParams = params
        mExoPlayerFullscreen = true

    }

    private fun portraitPlayer() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mFullScreenIcon?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.fullscreen))
        val params = binding.rootNew.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = (230 * resources.displayMetrics.density).toInt()
        binding.rootNew.layoutParams = params
        mExoPlayerFullscreen = false
    }

    override fun onBackPressed() {
        if (mExoPlayerFullscreen) {
            portraitPlayer()
        } else {
            super.onBackPressed()
        }
    }


    private fun showSpeedOptions(view: View) {

        val popupMenu = PopupMenu(this, view, R.style.MyPopupMenu)
        val menu: Menu = popupMenu.menu
        val speeds = listOf("0.5x", "1x", "1.5x", "2x")
        speeds.map {
            menu.add(it)
        }
        popupMenu.setOnMenuItemClickListener { item ->
            player?.playbackParameters =
                PlaybackParameters(item.title.toString().replace("x", "f").toFloat())
            false
        }
        popupMenu.show()
    }


    private fun getPlayer(mediaSource: MediaSource) {
        val needNewPlayer = player == null
        if (needNewPlayer) {
            trackSelectorParameters = DefaultTrackSelector.ParametersBuilder().build()

            trackSelector = DefaultTrackSelector(this)
            trackSelector?.apply {
                parameters = trackSelectorParameters
                player = ExoPlayer.Builder(this@VideoPlayer).setTrackSelector(this).build()
                player?.addListener(playerEventListener)
                player?.addAnalyticsListener(EventLogger(trackSelector))
            }


            binding.exoPlayer.player = player
            player?.playWhenReady = shouldAutoPlay

            (binding.exoPlayer.videoSurfaceView as SurfaceView).setSecure(true)


            val haveStartPosition = resumeWindow !== C.INDEX_UNSET
            if (haveStartPosition) {
                player!!.seekTo(resumeWindow, resumePosition)
            }

            player!!.prepare(mediaSource, !haveStartPosition, false)


        }
    }

    private val isPlaying: Boolean
        get() = (player?.playbackState != Player.STATE_ENDED) && (player?.playbackState == Player.STATE_READY) && player?.playWhenReady ?: false


    private val playerEventListener: Player.Listener = object : Player.Listener {


        override fun onLoadingChanged(isLoading: Boolean) {
            //TODO: Please refer to the ExoPlayer guide.
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            when (playbackState) {
                ExoPlayer.STATE_IDLE -> {
                    showHideView(binding.progressBar, false)
                }

                ExoPlayer.STATE_BUFFERING -> {
                    showHideView(binding.progressBar, true)
                }

                ExoPlayer.STATE_READY -> {

                    showHideView(binding.progressBar, false)
                }

                ExoPlayer.STATE_ENDED -> {
                    showHideView(binding.progressBar, false)
                }

                else -> {
                    Log.d(
                        "onPlayerStateChanged",
                        "onPlayerStateChanged with playbackState = $playbackState"
                    )
                }
            }


        }

        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onPlayerError(error: PlaybackException) {
            if (binding.progressBar.isShown) {
                binding.progressBar.visibility = View.GONE
            }

            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                clearResumePosition()
                if (liveurl.isNotEmpty()) {
                    val mediaSource = buildMediaSource(Uri.parse(liveurl), applicationContext)
                    getPlayer(mediaSource)
                } else {
                    if (onlinePlayList.size > 0) {
                        val licenceUrl =
                            VideoPlayerInit.getInstance(this@VideoPlayer)?.prePareLicense(
                                    siteId, accesskey, mediaId, userId, 30, C.WIDEVINE_UUID
                                )
                        licenceUrl?.let { it1 -> playDrmVideo(vodUrl, it1) }
                    }
                }
            } else {
                Log.d(
                    "onPlayerError",
                    "onPlayerError: " + (error as ExoPlaybackException).cause?.message
                )
                updateResumePosition()
                pausePlayer()

            }


        }

        /*************************end code on 25-12-2020 */
        override fun onPositionDiscontinuity(reason: Int) {}
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
        override fun onSeekProcessed() {}
    }

    fun buildDataSourceFactory(context: Context): DataSource.Factory {
        val httpDataSourceFactory = buildHttpDataSourceFactory(context)
        return DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    private fun buildHttpDataSourceFactory(context: Context): HttpDataSource.Factory {
        return if (cookieValue != "" && is_live.equals("1")) {
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mapOf("Cookie" to cookieValue))
                .setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
        } else {
            DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
        }
    }


    private fun buildMediaSource(uri: Uri, context: Context): MediaSource {
        return when (Util.inferContentType(uri)) {

            C.TYPE_SS -> SsMediaSource.Factory(
                DefaultSsChunkSource.Factory((buildDataSourceFactory(context))),
                buildDataSourceFactory(context)
            ).createMediaSource(MediaItem.fromUri(uri))

            C.TYPE_HLS -> HlsMediaSource.Factory((buildDataSourceFactory(context)))
                .createMediaSource(MediaItem.fromUri(uri))

            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(
                (buildDataSourceFactory(
                    context
                ))
            ).createMediaSource(MediaItem.fromUri(uri))

            else -> {
                throw IllegalStateException("Unsupported type:")
            }
        }
    }

    fun showHideView(v: View, show: Boolean) {
        if (show) {
            v.visibility = View.VISIBLE
        } else {
            v.visibility = View.GONE
        }
    }


    private fun pausePlayer() {
        player?.let {
            it.playWhenReady = false
            it.playbackState
        }
    }

    private fun updateResumePosition() {
        player?.let {
            resumeWindow = it.currentWindowIndex
            resumePosition =
                if (it.isCurrentWindowSeekable) 0.coerceAtLeast(it.currentPosition.toInt())
                    .toLong() else C.TIME_UNSET
        }

    }

    private fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun playDrmVideo(url: String, licenceUrl: String) {
        vodUrl = url
        val defaultHttpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, getString(R.string.app_name)))
            .setTransferListener(
                DefaultBandwidthMeter.Builder(this).setResetOnNetworkTypeChange(false).build()
            )
        val dashChunkSourceFactory: DashChunkSource.Factory =
            DefaultDashChunkSource.Factory(defaultHttpDataSourceFactory)
        val manifestDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, getString(R.string.app_name)))
        val dashMediaSource =
            DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
                .createMediaSource(
                    MediaItem.Builder().setUri(Uri.parse(url)) // DRM Configuration
                        .setDrmConfiguration(
                            DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(licenceUrl)
                                .build()
                        ).setMimeType(MimeTypes.APPLICATION_MPD).setTag(null).build()
                )
        getPlayer(dashMediaSource)
    }

    private inline fun <reified T> Gson.fromJsonList(json: String): List<T> =
        fromJson(json, object : TypeToken<List<T>>() {}.type)


    override fun initializePlayer(url: String?, responce: String?) {
        responce?.let {
            try {
                JSONObject(responce).let {
                    it.has("is_live").let { status ->
                        if (status) {
                            is_live = it.getString("is_live")
                        }
                    }
                    it.has("cookies").apply {
                        if (this) {
                            it.getJSONObject("cookies").let { it ->
                                it.has("CloudFront-Policy").apply {
                                    if (this) {
                                        it.getString("CloudFront-Policy").let {
                                            cookieValue += "CloudFront-Policy=$it;"
                                        }
                                    } else {
                                        cookieValue += "CloudFront-Policy=;"
                                    }
                                }

                                it.has("CloudFront-Signature").apply {
                                    if (this) {
                                        it.getString("CloudFront-Signature").let {
                                            cookieValue += "CloudFront-Signature=$it;"
                                        }
                                    } else {
                                        cookieValue += "CloudFront-Signature=;"
                                    }
                                }

                                it.has("CloudFront-Key-Pair-Id").apply {
                                    if (this) {
                                        it.getString("CloudFront-Key-Pair-Id").let {
                                            cookieValue += "CloudFront-Key-Pair-Id=$it;"
                                        }
                                    } else {
                                        cookieValue += "CloudFront-Key-Pair-Id=;"
                                    }
                                }
                            }
                        }
                    }
                    if (url.toString().contains("m3u8")) {
                        url?.let {
                            liveurl = url
                        }
                        val mediaSource = buildMediaSource(Uri.parse(url), this)
                        getPlayer(mediaSource)
                    } else {

                        if (it.has("meta") && it.getJSONArray("meta").length() > 0) {
                            downloadList.clear()
                            onlinePlayList.clear()
                            val list =
                                Gson().fromJsonList<UrlResponse>(it.getJSONArray("meta").toString())
                            list.map { it ->
                                if (it.type == "mp4") {
                                    downloadList.add(it)
                                }
                                if (it.type == "mpd") {
                                    onlinePlayList.add(it)
                                }
                            }
                            if (onlinePlayList.size > 0) {
                                val licenceUrl =
                                    VideoPlayerInit.getInstance(this@VideoPlayer)?.prePareLicense(
                                            siteId, accesskey, mediaId, userId, 30, C.WIDEVINE_UUID
                                        )
                                licenceUrl?.let { it1 -> playDrmVideo(onlinePlayList[0].url, it1) }
                            }
                            if (downloadList.size > 0) {
                                binding.download.isVisible = true
                            }
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


    }


}