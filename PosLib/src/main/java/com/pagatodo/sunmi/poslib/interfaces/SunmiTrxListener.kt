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
    fun onDialogRequestCard(message: String? = null, cardTypes: Int = 0)
    fun onDismissRequestCard()
    fun onDialogProcessOnline(message: String? = null)
    fun onDismissRequestOnline()
    fun onShowSingDialog(doContinue: (ByteArray) -> Unit)
    fun createTransactionData(): TransactionData
    fun pinMustBeForced(): Boolean
    fun requireSignature(dataCard: DataCard): Boolean
    fun isPossibleFallback(): Boolean
    fun checkCardTypes(): Int
    fun onShowTicketDialog(responseTrx: Respuesta?, dataCard: DataCard, singBytes: ByteArray? = null)
    fun onSuccessOnline(doContinue: () -> Unit)
    fun onShowDniDialog(dataCard: DataCard)
    fun onShowZipDialog(dataCard: DataCard)
    fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3)
    fun onShowSelectApp(listEmvApps: List<String>, appSelect: (Int) -> Unit)
    fun onSync(dataCard: DataCard)
    fun onFailureEmv(error: PosResult, todo: (String) -> Unit)
    fun onFailureOnline(error: PosResult, doContinue: () -> Unit)
    fun onPurchase(dataCard: DataCard)
    fun doOperationNext(nextOperation: OperacionSiguiente, message: String, doContinue: () -> Unit)
    fun getVmodelPCI(): SunmiViewModel<E>
    fun showReading()
    fun showRemoveCard(dataCard: DataCard?)
}