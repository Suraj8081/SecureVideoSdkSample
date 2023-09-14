package com.example.securevideosdksample.download.interfaces

import com.example.securevideosdksample.room.table.DownloadVideoTable

interface OnItemClick {
    fun OnVideoClick(pos: Int, videoDownload: DownloadVideoTable?, type: String?)
    fun clickOnDownloadVide(downloadVideoTable: DownloadVideoTable)
}