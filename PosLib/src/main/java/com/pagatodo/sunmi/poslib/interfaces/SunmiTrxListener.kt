package com.pagatodo.sunmi.poslib.interfaces

import com.pagatodo.sunmi.poslib.config.PinPadConfigV3
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslib.viewmodel.AbstractViewModel
import com.pagatodo.sunmi.poslib.viewmodel.EmvViewModel
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Menu
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Operaciones
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Productos
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.Respuesta

interface SunmiTrxListener<E : Any> {
    fun onDialogRequestCard(message: String? = null, cardTypes: Int = 0)
    fun onDismissRequestCard()
    fun onDialogProcessOnline(message: String? = null, dataCard: DataCard? = null)
    fun onDismissRequestOnline()
    fun onShowSingDialog(ref:String, doContinue: (ByteArray) -> Unit)
    fun createTransactionData(): TransactionData
    fun pinMustBeForced(): Boolean
    fun getPinLength(): Int
    fun requireSignature(dataCard: DataCard): Boolean
    fun isPossibleFallback(): Boolean
    fun checkCardTypes(): Int
    fun onShowTicketDialog(singBytes: ByteArray? = null, responseTrx: Respuesta?, dataCard: DataCard, onShowed: (operation: Operaciones, product: Productos, menu: Menu) -> Unit)
    fun onSuccessOnline(doContinue: () -> Unit)
    fun onShowDniDialog(dataCard: DataCard)
    fun onShowZipDialog(dataCard: DataCard)
    fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3)
    fun onShowSelectApp(listEmvApps: List<String>, appSelect: (Int) -> Unit)
    fun onSync(dataCard: DataCard)
    fun onFailureEmv(error: PosResult, todo: (String) -> Unit)
    fun onFailureOnline(error: PosResult, doContinue: (String) -> Unit)
    fun onPurchase(dataCard: DataCard)
    fun doOperationNext(nextOperation: OperacionSiguiente, message: String, doContinue: (String) -> Unit)
    fun getVmodelPCI(): AbstractViewModel<E>
    fun showReading()
    fun possibleCancelCheckCard(isPossible: Boolean)
    fun showRemoveCard(dataCard: DataCard?)
    fun verifyServiceCode(): Boolean
    fun createParamV2(): EmvTermParamV2
    fun sendTicketSever(responseTrx: Respuesta, dataCard: DataCard, doContinue: (Boolean) -> Unit)
    fun onSaveTransaction(operation: Operaciones, product: Productos, menu: Menu, responseTrx: Respuesta, doContinue: (Long) -> Unit)
    fun eraseDb(long: Long)
}