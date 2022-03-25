package com.pagatodo.sunmi.poslib.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private var syncDao: SyncDao = SyncDatabase.getDatabase(application).databaseDao()
     var syncLiveData: LiveData<List<Sync>>? = null

    suspend fun insertSyncData(sync: Sync) = syncDao.insert(sync)

    fun getByStatus(status: String, liveData: MutableLiveData<List<Sync>>){
        viewModelScope.launch(Dispatchers.IO) {
            liveData.postValue(
                syncDao.selectByStatus(status)
            )
        }
    }

    fun deleteAll() {
        viewModelScope.launch { syncDao.deleteSyncData() }
    }
}