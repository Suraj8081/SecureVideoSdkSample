package com.example.securevideosdksample.room.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securevideosdksample.room.CareerwillDatabase
import com.example.securevideosdksample.room.table.DownloadVideoTable
import kotlinx.coroutines.launch


class DownloadVideoModel(private val sanskritiDatabase: CareerwillDatabase?) : ViewModel() {
    private val _data = MutableLiveData<List<DownloadVideoTable>?>()
    val data: LiveData<List<DownloadVideoTable>?> get() = _data

    fun getAllUserData(userId: String, courseId: String) {
        sanskritiDatabase?.downloadDao()?.let {
            viewModelScope.launch {
                _data.postValue(it.getAllData(userId, courseId))
            }
        }
    }

    fun removeObserverData() {
        _data.value = null

    }

}