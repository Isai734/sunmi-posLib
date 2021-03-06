package com.pagatodo.sunmi.poslib

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.harmonizer.SyncService
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.keychain.CryptUtil
import com.pagatodo.sunmi.poslib.util.EmvUtil
import com.pagatodo.sunmi.poslib.util.MoshiInstance
import com.pagatodo.sunmi.poslib.util.StatusTrx
import com.pagatodo.sunmi.poslib.viewmodel.SyncViewModel

class PosLib private constructor(val activity: Activity) : SunmiServiceWrapper() {

    var posConfig = PosConfig()
    lateinit var encryptUtil: CryptUtil
    var user: String = ""

    init {
        connectPayService(activity)
    }

    private fun setGlobalConfig() {
        mSecurityOptV2?.apply { EmvUtil.initKey(this) }
        encryptUtil = mSecurityOptV2?.let { CryptUtil(it) }
            ?: throw IllegalStateException("SecurityOptV2 is null.")
        mEMVOptV2?.apply {
            EmvUtil.setAids(this)
            EmvUtil.setCapks(this)
            EmvUtil.setDlr(this)
        }
    }

    companion object {

        val TAG: String = PosLib::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: PosLib? = null

        fun createInstance(activity: Activity): PosLib = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PosLib(activity).also { INSTANCE = it }
        }

        fun loadGlobalConfig(posConfig: PosConfig) {
            val posLib = getInstance()
            posLib.posConfig = posConfig
            posLib.setGlobalConfig()
        }

        fun validateSync(observer: Observer<WorkInfo>){

        }

        fun getInstance(): PosLib {
            return INSTANCE ?: throw IllegalStateException("You need to create Instance PosLib.")
        }
    }
}

internal fun requireContext() = PosLib.getInstance().activity

@Suppress("DEPRECATION")
fun Activity.setFullScreen(){
    val decorView = window.decorView
    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
}

fun AppCompatActivity.validateSync(observer: Observer<WorkInfo>){
    val serviceBd by lazy { ViewModelProvider(this)[SyncViewModel::class.java] }
    val liveData = MutableLiveData<List<Sync>>()
    val moshi = MoshiInstance.create()
    liveData.observe(this){
        Log.d(PosLib.TAG, "find sync ${it.size}")
        for (sync in it){
            Log.d(PosLib.TAG, "find sync $sync")
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val syncWorker: WorkRequest = OneTimeWorkRequestBuilder<SyncService>()
                .setInputData(workDataOf(
                    SyncService.KEY_INPUT_DATA to moshi.adapter(Sync::class.java).toJson(sync)
                ))
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(requireContext()).enqueue(syncWorker)
            WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(syncWorker.id).observe(this, observer)
        }
    }
    serviceBd.getByStatus(StatusTrx.PROGRESS.name, liveData)
}


fun posInstance() = PosLib.getInstance()