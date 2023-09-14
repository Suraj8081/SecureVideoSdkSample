package com.example.securevideosdksample.download.adapter


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securevideosdksample.R
import com.example.securevideosdksample.databinding.DownloadedVideoItemsBinding
import com.example.securevideosdksample.download.interfaces.OnItemClick
import com.example.securevideosdksample.room.table.DownloadVideoTable


class DownloadAdapter(
    val context: Activity, private val onItemClick: OnItemClick, val userId: String
) : ListAdapter<DownloadVideoTable, DownloadAdapter.MyViewHolder>(
    MyDownloadsDiffUtil()
) {

    inner class MyViewHolder(val binding: DownloadedVideoItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            DownloadedVideoItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        val videosDownload = getItem(holder.absoluteAdapterPosition)
        if (payloads.isEmpty()) {
            try {

                holder.binding.videoTitle.text = videosDownload.name
                holder.binding.videoTitle.isSelected = true
                holder.binding.fileMb.text = videosDownload.lengthInMb


                if (videosDownload.percentage == 100) {
                    holder.binding.deleteVideo.isVisible = true
                    holder.binding.progressCvr.visibility = View.GONE
                } else {
                    holder.binding.pauseBtn.isVisible = true
                    holder.binding.deleteVideo.isVisible = false
                    holder.binding.progressCvr.visibility = View.VISIBLE
                    holder.binding.progressValue.progress = videosDownload.percentage
                    videosDownload?.videoStatus?.let {
                        when (it) {
                            "Downloading Pause" -> {
                                holder.binding.pauseBtn.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context, R.drawable.play_button
                                    )
                                )
                            }

                            "Downloading Running" -> {
                                holder.binding.pauseBtn.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context, R.drawable.ic_video_download_pause
                                    )
                                )
                            }

                        }
                    }

                    holder.binding.percentageValue.text = "${videosDownload.percentage} % Done"
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val o = payloads[0] as Bundle
            holder.binding.fileMb.text = o.getString("lengthInMb")

            if (o.getInt("percentage") == 100) {
                holder.binding.deleteVideo.isVisible = true
                holder.binding.progressCvr.visibility = View.GONE
            } else {
                holder.binding.deleteVideo.isVisible = false
                holder.binding.progressCvr.visibility = View.VISIBLE
                holder.binding.progressValue.progress = o.getInt("percentage")
                holder.binding.percentageValue.text = "${o.getInt("percentage")} % Done"
                when (o.getString("video_status")) {
                    "Downloading Pause" -> {
                        holder.binding.pauseBtn.setImageDrawable(
                            ContextCompat.getDrawable(
                                context, R.drawable.play_button
                            )
                        )
                    }

                    "Downloading Running" -> {
                        holder.binding.pauseBtn.setImageDrawable(
                            ContextCompat.getDrawable(
                                context, R.drawable.ic_video_download_pause
                            )
                        )
                    }

                }

            }
        }

        holder.binding.deleteVideo.setOnClickListener {
            android.app.AlertDialog.Builder(context).setTitle("Delete entry")
                .setMessage("Are You Sure You Want to Delete Video ?")
                .setPositiveButton("yes") { _, _ ->
                    (onItemClick.OnVideoClick(
                        holder.absoluteAdapterPosition, videosDownload, "CANCEL"
                    ))
                }.setNegativeButton("No", null).setIcon(android.R.drawable.ic_dialog_alert).show()


        }

        holder.binding.pauseBtn.setOnClickListener {
            videosDownload?.videoStatus?.let {
                when (it) {
                    "Downloading Running" -> {
                        (onItemClick.OnVideoClick(
                            holder.absoluteAdapterPosition, videosDownload, "PAUSE"
                        ))
                    }

                    "Downloading Pause" -> {
                        (onItemClick.OnVideoClick(
                            holder.absoluteAdapterPosition, videosDownload, "RESUME"
                        ))
                    }

                }

            }
        }

        holder.binding.cancelBtn.setOnClickListener {
            android.app.AlertDialog.Builder(context).setTitle("Delete entry")
                .setMessage("Are You Sure You Want to Delete Video ?")
                .setPositiveButton("yes") { _, _ ->
                    (onItemClick.OnVideoClick(
                        holder.absoluteAdapterPosition, videosDownload, "CANCEL"
                    ))
                }.setNegativeButton("No", null).setIcon(android.R.drawable.ic_dialog_alert).show()


        }

        holder.binding.parentLayout.setOnClickListener {
            onItemClick.clickOnDownloadVide(videosDownload)
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    }
}

class MyDownloadsDiffUtil : DiffUtil.ItemCallback<DownloadVideoTable>() {
    override fun areItemsTheSame(
        oldItem: DownloadVideoTable, newItem: DownloadVideoTable
    ): Boolean {
        return oldItem.videoId == newItem.videoId
    }

    override fun areContentsTheSame(
        oldItem: DownloadVideoTable, newItem: DownloadVideoTable
    ): Boolean {
        return oldItem.percentage == newItem.percentage && oldItem.lengthInMb == newItem.lengthInMb && oldItem.videoStatus == newItem.videoStatus
    }

    override fun getChangePayload(oldItem: DownloadVideoTable, newItem: DownloadVideoTable): Any? {
        val bundle = Bundle()
        if (oldItem.percentage != newItem.percentage) {
            bundle.putInt("percentage", newItem.percentage)
            bundle.putString("lengthInMb", newItem.lengthInMb)
            bundle.putString("video_status", newItem.videoStatus)
        }
        return if (bundle.size() == 0) {
            null
        } else bundle

    }
}

