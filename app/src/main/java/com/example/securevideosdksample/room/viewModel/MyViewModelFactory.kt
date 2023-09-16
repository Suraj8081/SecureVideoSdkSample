package com.example.securevideosdksample.room.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.securevideosdksample.room.CareerwillDatabase

class MyViewModelFactory(private val sanskritiDatabase: CareerwillDatabase?) : ViewModelProvider.Factory {

     override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(DownloadVideoModel::class.java)) {
            DownloadVideoModel(this.sanskritiDatabase) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}