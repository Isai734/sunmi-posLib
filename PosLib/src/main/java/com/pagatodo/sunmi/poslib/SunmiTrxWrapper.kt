package com.pagatodo.sunmi.poslib

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.pagatodo.sunmi.poslib.PosLib.Companion.TAG
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.interfaces.OnClickAcceptListener
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.util.*
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.Respuesta
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import net.fullcarga.android.api.exc.SincronizacionRequeridaException
import java.nio.charset.Charset

class SunmiTrxWrapper(owner: LifecycleOwner) :
    SunmiTransaction() {

    private val mTransactionData by lazy { sunmiListener.createTransactionData() }
    private lateinit var dataCard: DataCard
    private val sunmiListener: SunmiTrxListener<*>
    private var requestTransaction: RespuestaTrxCierreTurno? = null
    private var forceCheckCard: Int = -1

    init {
        if (owner is SunmiTrxListener<*>)
            sunmiListener = owner
        else
            throw InstantiationException("Owner must be instance of SunmiTrxListener.")
        sunmiListener.getVmodelPCI().pciViewModel.observe(owner, pciObserver)
        sunmiListener.getVmodelPCI().syncViewModel.observe(owner, syncObserver)
    }

    fun initTransaction() {
        setTerminalParams()
        forceCheckCard = -1
        sunmiListener.onDialogRequestCard()
        super.clearVars()
        super.startPayProcess()
    }

    private fun resendTransaction(title: String? = null) {
        sunmiListener.onDialogRequestCard(title)
        super.startPayProcess()
    }

    fun getPin() {
        getPin(dataCard)
    }

    override fun goOnlineProcess(dataCard: DataCard) {
        this.dataCard = dataCard.apply { PosLogger.d(TAG, this.toString()) }
        sunmiListener.onDismissRequestCard()
        sunmiListener.onDialogProcessOnline()
        sunmiListener.onPurchase(dataCard)
    }

    override fun onFailureTrx(result: PosResult) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onDismissRequestOnline()
        when (result) {
            PosResult.DoSyncOperation -> {
                sunmiListener.onDialogProcessOnline(result.message)
                sunmiListener.onSync(dataCard)
            }
            PosResult.ReplaceCard, PosResult.SeePhone, PosResult.CardNoSupported,
            PosResult.CardDenial, PosResult.NfcTerminated, PosResult.NextOperetion -> {
                sunmiListener.onFailure(result, listener = createAcceptListener(result.message))
            }
            PosResult.FallBack, PosResult.FallBackCommonApp -> {
                allowFallback = true
                forceCheckCard = mcrOnlyCheckCard
                sunmiListener.onFailure(result, listener = createAcceptListener(result.message))
            }
            PosResult.OtherInterface -> {
                forceCheckCard = rfOffCheckCard
                sunmiListener.onFailure(result, listener = createAcceptListener(result.message))
            }
            else -> sunmiListener.onFailure(result)
        }
    }

    override fun onApprovedTrx() {
        sunmiListener.onDismissRequestOnline()
        if (isRequestSignature || VentaPCIUtils.emvRequestSignature(dataCard.tlvData))
            sunmiListener.onShowSingDialog(requestTransaction as Respuesta, dataCard)
        else
            sunmiListener.onShowTicketDialog(null, requestTransaction as Respuesta, dataCard)
    }

    override fun getCheckCardType(): Int {
        return if (forceCheckCard == -1)
            sunmiListener.checkCardTypes()
        else
            forceCheckCard
    }

    override fun pinMustBeForced() = sunmiListener.pinMustBeForced()

    override fun readingCard() = sunmiListener.showReading()

    override fun onShowPinPad(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onShowPinPadDialog(pinPadListener, pinPadConfig)
    }

    override fun onSelectEmvApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onShowSelectApp(listEmvApps, applicationEmv)
    }

    override fun getTransactionData() = mTransactionData

    override fun onRemoveCard() = sunmiListener.showRemoveCard()

    private fun doNextOpr(operacionSiguiente: OperacionSiguiente, nextOprResult: PosResult) {
        cancelProcessEmv()
        sunmiListener.doOperationNext(operacionSiguiente, nextOprResult)
    }

    private fun createAcceptListener(message: String? = null) = object : OnClickAcceptListener {
        override fun onClickAccept(view: View?) {
            resendTransaction(message)
        }
    }

    private val pciObserver
        get() = Observer<Results<Any>> {
            when (it) {
                is Results.Success -> {
                    if (it.data is RespuestaTrxCierreTurno) {
                        requestTransaction = it.data
                        if (it.data.isCorrecta) {
                            if (validateNextOpr(it.data.operacionSiguiente)) {
                                val resultNexOpr = PosResult.NextOperetion
                                resultNexOpr.message = it.data.campo60.first()
                                doNextOpr(it.data.operacionSiguiente, resultNexOpr)
                            } else {
                                val tags =
                                    String(it.data.campoTagsEmv, Charset.defaultCharset()).trim()
                                finishOnlineProcessStatus(
                                    tlvString = tags,
                                    tlvResponse = Constants.TlvResponses.Approved
                                )
                            }
                        } else finishOnlineProcessStatus(tlvResponse = Constants.TlvResponses.Decline)
                    } else finishOnlineProcessStatus(tlvResponse = Constants.TlvResponses.Approved)
                }
                is Results.Failure -> {
                    val msgError = if (it.exception is SincronizacionRequeridaException) {
                        onFailureTrx(PosResult.DoSyncOperation)
                        PosResult.DoSyncOperation.message
                    } else
                        it.exception.message
                    finishOnlineProcessStatus(
                        tlvResponse = Constants.TlvResponses.Decline,
                        message = msgError
                    )
                }
            }
        }

    private fun validateNextOpr(nxtOpr: OperacionSiguiente?): Boolean {
        return nxtOpr?.let { it.procodIdNext > 0 } ?: false
    }

    fun cancelProcess() {
        cancelProcessEmv()
    }

    fun cancelProcessWithMessage() {
        cancelOperationWithMessage()
    }

    fun cancelProcessWithTvrError() {
        finishOnlineProcessStatus(tlvResponse = Constants.TlvResponses.Decline)
    }

    private val rfOffCheckCard: Int
        get() = AidlConstants.CardType.MAGNETIC.value or AidlConstants.CardType.IC.value

    private val mcrOnlyCheckCard: Int
        get() = AidlConstants.CardType.MAGNETIC.value

    private val syncObserver
        get() = Observer<Results<Any>> {
            when (it) {
                is Results.Success -> {
                    if (it.data is RespuestaTrxCierreTurno) {
                        sunmiListener.onDismissRequestOnline()
                        onFailureTrx(getPosResult(it.data.error, it.data.msjError))
                    } else onFailureTrx(PosResult.SyncOperationSuccess)
                }
                is Results.Failure -> {
                    onFailureTrx(PosResult.SyncOperationFailed)
                }
            }
        }
}