package com.example.securevideosdksample.room.table

import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "downloadVideo")
data class DownloadVideoTable(
    var name: String = "",
    var courseId: String="",
    var userId: String = "",
    var videoStatus: String = "",
    var videoId: String = "",
    var videoUrl: String = "",
    var token: String = "",
    var percentage: Int = 0,
    var fileName: String = "",
    var isComplete: String = "0",
    var originalFileLengthString: String = "0",
    var lengthInMb: String = "",
    var totalDownloadLocale: Long = 0L,
    var thumbnail_url: String,

    ) : Parcelable {

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null


}