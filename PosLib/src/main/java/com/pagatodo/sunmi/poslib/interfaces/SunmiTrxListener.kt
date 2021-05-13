package com.pagatodo.sunmi.poslib.interfaces

import com.pagatodo.sunmi.poslib.config.PinPadConfigV3
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslib.viewmodel.SunmiViewModel
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.Respuesta

interface SunmiTrxListener<E : Any> {
    fun onDialogRequestCard(message: String? = null)
    fun onDismissRequestCard()
    fun onDialogProcessOnline(message: String? = null)
    fun onDismissRequestOnline()
    fun onShowSingDialog(responseTrx: Respuesta?, dataCard: DataCard)
    fun createTransactionData(): TransactionData
    fun pinMustBeForced(): Boolean
    fun checkCardTypes(): Int
    fun onShowTicketDialog(singBytes: ByteArray?, responseTrx: Respuesta?, dataCard: DataCard)
    fun onShowDniDialog(dataCard: DataCard)
    fun onShowZipDialog(dataCard: DataCard)
    fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3)
    fun onShowSelectApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener)
    fun onSync(dataCard: DataCard)
    fun onFailure(error: PosResult, listener: OnClickAcceptListener? = null)
    fun onPurchase(dataCard: DataCard)
    fun doOperationNext(nextOperation: OperacionSiguiente, nextOprResult: PosResult)
    fun getVmodelPCI(): SunmiViewModel<E>
    fun showReading()
    fun showRemoveCard(dataCard: DataCard?)
}