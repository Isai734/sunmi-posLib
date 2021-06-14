package com.pagatodo.sunmi.poslib

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.keychain.CryptUtil
import com.pagatodo.sunmi.poslib.util.EmvUtil

class PosLib private constructor(val activity: Activity) : SunmiServiceWrapper() {

    var posConfig = PosConfig()
    lateinit var encryptUtil: CryptUtil

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
fun posInstance() = PosLib.getInstance()