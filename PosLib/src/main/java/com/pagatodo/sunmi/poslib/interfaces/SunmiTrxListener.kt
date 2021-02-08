package com.pagatodo.sunmi.poslib.interfaces

import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslib.viewmodel.SunmiViewModel
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente

interface SunmiTrxListener<E: Any> {
    fun onDialogRequestCard(message: String? = null)
    fun onDismissRequestCard()
    fun onDialogProcessOnline(message: String? = null)
    fun onDismissRequestOnline()
    fun onShowSingDialog()
    fun createTransactionData(): TransactionData
    fun pinMustBeForced(): Boolean
    fun checkCardTypes(): Int
    fun onShowTicketDialog(singBytes: ByteArray?)
    fun onShowDniDialog()
    fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2)
    fun onShowSelectApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener)
    fun onSync(dataCard: DataCard)
    fun onFailure(error: PosResult, listener: OnClickAcceptListener? = null)
    fun onPurchase(dataCard: DataCard)
    fun doOperationNext(nextOperation: OperacionSiguiente)
    fun getVmodelPCI(): SunmiViewModel<E>
}