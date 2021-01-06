package com.pagatodo.sunmi.poslib.interfaces

import com.pagatodo.sunmi.poslib.util.PosResult
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2

interface SunmiTrxListener {
    fun onShowRequestCard()
    fun onDismissRequestCard()
    fun onShowProcessOnline()
    fun onDismissRequestOnline()
    fun onShowSingDialog()
    fun onShowDniDialog()
    fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2)
    fun onShowSelectApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener)
    fun <E> onSuccess(request: E)
    fun onFailure(result: PosResult)
}