package com.pagatodo.sunmi.poslib.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    var syncDao: SyncDao = SyncDatabase.getDatabase(application).databaseDao()
    private var syncLiveData: LiveData<List<Sync>>? = null

    fun insertSyncData(sync: Sync){
        viewModelScope.launch { syncDao.insert(sync) }
    }

    fun getByStatus(status: String) {
        syncLiveData = syncDao.selectByStatus(status)
    }

    fun deleteAll() {
        viewModelScope.launch { syncDao.deleteSyncData() }
    }
}