package com.example.securevideosdksample.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.securevideosdksample.room.table.DownloadVideoTable

@Dao
interface DownloadDao {

    @Insert
    suspend fun insert(user: DownloadVideoTable): Long


    @Query("SELECT * FROM downloadVideo  WHERE userId = :userId AND courseId =:courseId ")
    suspend fun getAllData(userId: String, courseId: String): List<DownloadVideoTable>

    @Query("SELECT * FROM downloadVideo  WHERE userId = :userId AND courseId=:courseId")
    suspend fun getAllUserData(userId: String, courseId: String?): List<DownloadVideoTable>

    @Query("SELECT * FROM downloadVideo  WHERE videoId = :videoId AND isComplete=:complete  and userId =:userid and courseId =:courseId")
    suspend fun getVideoData(
        videoId: String?, complete: String? = "0", userid: String?, courseId: String?
    ): DownloadVideoTable?

    @Query("SELECT * FROM downloadVideo  WHERE videoId = :videoId AND userId =:userid AND courseId =:courseId")
    suspend fun getVideoData(
        videoId: String?, userid: String?, courseId: String?
    ): DownloadVideoTable?

    @Query("UPDATE downloadVideo SET  videoStatus =:status , percentage =:percentage  , isComplete =:isComplete  WHERE videoId = :videoId ANd userId=:userid AND courseId=:courseId")
    suspend fun updateVideoStatus(
        videoId: String?,
        isComplete: String?,
        status: String?,
        percentage: Int,
        userid: String,
        courseId: String?
    ): Int

    @Query("SELECT * FROM downloadVideo  WHERE videoId = :videoid AND isComplete =:complete  AND userId =:userid AND courseId =:courseId")
    suspend fun getVideoIsComplete(
        videoid: String?, userid: String?, complete: String?, courseId: String?
    ): DownloadVideoTable?


    @Query("SELECT EXISTS(SELECT * FROM downloadVideo WHERE videoId = :video_id  AND userId = :userId AND courseId = :course_id)")
    suspend fun isRecordExist(video_id: String?, userId: String?, course_id: String?): Boolean


    @Query("SELECT * FROM downloadVideo  WHERE userId = :userId AND courseId = :courseId")
    fun getAllVideos(userId: String, courseId: String?): List<DownloadVideoTable>


    @Query("SELECT * FROM downloadVideo")
    fun getAllUserData(): LiveData<List<DownloadVideoTable>>


    @Query("SELECT * FROM downloadVideo  WHERE videoId = :videoId AND isComplete=:complete  and userId =:userid and courseId =:courseId")
    fun getVideo(
        videoId: String?, complete: String? = "0", userid: String?, courseId: String?
    ): DownloadVideoTable?


    @Query("UPDATE downloadVideo SET  videoStatus =:status , isComplete =:isconpelte  WHERE videoId = :videoid  AND userId =:userid AND courseId =:courseId")
    fun updateStatusProgress(
        videoid: String?, isconpelte: String?, status: String?, userid: String?, courseId: String?
    )


    @Query("SELECT EXISTS(SELECT * FROM downloadVideo WHERE videoId = :video_id AND isComplete = :is_complete  AND userId = :userId AND courseId = :courseId)")
    fun isRecordExistsUserId(
        video_id: String?, is_complete: String?, userId: String?, courseId: String?
    ): Boolean

    @Query("UPDATE downloadVideo SET   videoUrl=:mp4ulr , originalFileLengthString=:originalFileLengthString  , videoStatus=:status  , totalDownloadLocale =:total , lengthInMb =:lengthInMb , percentage =:percentage  , isComplete =:isComplete WHERE videoId = :videoId     AND isComplete = :isComplete AND courseId = :courseId")
    fun updateVideoFileLength(
        videoId: String?,
        originalFileLengthString: String?,
        total: Long?,
        lengthInMb: String?,
        percentage: Int,
        isComplete: String?,
        status: String?,
        mp4ulr: String?,
        courseId: String?
    ): Int


    @Query("UPDATE downloadVideo SET  videoStatus =:status , userId =:userId    WHERE videoId = :videoid AND courseId = :courseId")
    fun updateVideoStatus(videoid: String, status: String, userId: String, courseId: String?)

    @Query("Delete from downloadVideo where videoId = :videoid AND userId=:userid AND courseId=:courseId")
    fun deleteVideo(videoid: String?, userid: String?, courseId: String?): Int

    @Query("Delete from downloadVideo where videoId = :videoid AND userId=:userid AND courseId=:courseId")
    suspend fun deleteVideoSuspend(videoid: String?, userid: String?, courseId: String?): Int

    @Query("UPDATE downloadVideo SET videoUrl =:url, videoStatus =:status , percentage =:percentage  , isComplete =:isComplete  WHERE videoId = :videoId AND courseId=:courseId")
    fun updateVideoStatus(
        videoId: String?,
        isComplete: String?,
        status: String?,
        percentage: Int,
        url: String?,
        courseId: String?
    )



}