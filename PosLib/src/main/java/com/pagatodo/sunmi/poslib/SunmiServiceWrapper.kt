package com.pagatodo.sunmi.poslib

import android.content.Context
import com.pagatodo.sunmi.poslib.util.PosLogger
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2
import sunmi.paylib.SunmiPayKernel

open class SunmiServiceWrapper {
    protected val mSecurityOptV2: SecurityOptV2? get() = mSMPayKernel?.mSecurityOptV2
    private var mSMPayKernel: SunmiPayKernel? = null
    val mBasicOptV2: BasicOptV2? get() = mSMPayKernel?.mBasicOptV2
    val mReadCardOptV2: ReadCardOptV2? get() = mSMPayKernel?.mReadCardOptV2
    val mEMVOptV2: EMVOptV2? get() = mSMPayKernel?.mEMVOptV2
    val mPinPadOptV2: PinPadOptV2? get() = mSMPayKernel?.mPinPadOptV2

    protected fun connectPayService(context: Context) {
        mSMPayKernel = SunmiPayKernel.getInstance()
        mSMPayKernel?.initPaySDK(context, object : SunmiPayKernel.ConnectCallback {
            override fun onConnectPaySDK() {
                PosLogger.i(PosLib.TAG, "Pay Sdk connect")
            }

            override fun onDisconnectPaySDK() {
                PosLogger.w(PosLib.TAG, "Pay Sdk disconnect")
            }
        })
    }

    fun screenFinancialModel() {
        val uid: Int = requireContext().applicationInfo!!.uid
        try {
            mBasicOptV2?.setScreenMode(uid)
        } catch (e: Exception) {
            PosLogger.w(PosLib.TAG, e.message)
        }
    }



    fun screenMonopoly() {
        try {
            mBasicOptV2?.setScreenMode(-1)
        } catch (e: Exception) {
            PosLogger.w(PosLib.TAG, e.message)
        }
    }

}