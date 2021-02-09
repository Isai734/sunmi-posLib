package com.pagatodo.sunmi.poslib

import android.annotation.SuppressLint
import android.app.Activity
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.keychain.CryptUtil
import com.pagatodo.sunmi.poslib.util.EmvUtil

class PosLib private constructor(val activity: Activity) : SunmiServiceWrapper() {

    var posConfig = PosConfig()
    var encryptUtil = mSecurityOptV2?.let { CryptUtil(it) }
        ?: throw IllegalStateException("SecurityOptV2 is null.")

    init {
        connectPayService(activity)
    }

    private fun setGlobalConfig() {
        mSecurityOptV2?.apply { EmvUtil.initKey(this) }
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

fun posInstance() = PosLib.getInstance()