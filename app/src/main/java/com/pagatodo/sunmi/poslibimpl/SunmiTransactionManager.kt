package com.pagatodo.sunmi.poslibimpl

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pagatodo.sunmi.poslib.SunmiTransaction
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.util.Constants
import com.pagatodo.sunmi.poslib.util.EmvUtil
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslib.util.getPosResult
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import java.nio.charset.Charset

class SunmiTransactionManager(private val activity: AppCompatActivity) :
    SunmiTransaction() {

    private val mTransactionData = TransactionData()
    private lateinit var sunmiListener: SunmiTrxListener
    private var requestTransaction: RespuestaTrxCierreTurno? = null

    private val viewModelPci: ViewModelPci by lazy {
        ViewModelProvider(activity)[ViewModelPci::class.java].apply {
            purchaseMlData.observe(activity, pciObserver)
        }
    }

    fun initTransaction(tAmount: String) {
        with(mTransactionData) {
            transType = Constants.TransType.PURCHASE
            amount = tAmount
            totalAmount = tAmount
            otherAmount = "00"
            currencyCode = "0156"
            cashBackAmount = "00"
            taxes = "00"
            comisions = "00"
            gratuity = "00"
            sigmaOperation = "V"
            tagsEmv = EmvUtil.tagsDefault.toList()
        }
        sunmiListener.onShowRequestCard()
        super.startPayProcess()
    }

    override fun goOnlineProcess(dataCard: DataCard) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onShowProcessOnline()
        viewModelPci.purchase()
    }

    override fun onFailureTrx(result: PosResult) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onDismissRequestOnline()
        sunmiListener.onFailure(result)
    }

    override fun onApprovedTrx() {
        sunmiListener.onDismissRequestOnline()
        sunmiListener.onSuccess(requestTransaction)
    }

    override fun getCheckCardType(): Int {
        return AidlConstants.CardType.MAGNETIC.value or AidlConstants.CardType.IC.value or AidlConstants.CardType.NFC.value
    }

    override fun pinMustBeForced(): Boolean {
        return false
    }

    override fun onShowPinPad(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onShowPinPadDialog(pinPadListener, pinPadConfig)
    }

    override fun onSelectEmvApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener) {
        sunmiListener.onDismissRequestCard()
        sunmiListener.onShowSelectApp(listEmvApps, applicationEmv)
    }

    override fun getTransactionData() = mTransactionData

    val pciObserver = Observer<Results<RespuestaTrxCierreTurno>> {
        when (it) {
            is Results.Success -> {
                requestTransaction = it.data
                val tags = String(it.data.campoTagsEmv, Charset.defaultCharset()).trim()
                finishOnlineProcessStatus(tlvString = tags, tlvResponse = Constants.TlvResponses.Approved)
            }
            is Results.Failure -> {
                finishOnlineProcessStatus(tlvResponse = Constants.TlvResponses.Decline, message = it.exception.message)
            }
        }
    }

    val syncObserver = Observer<Results<AbstractRespuesta>> {
        when (it) {
            is Results.Success -> {
                sunmiListener.onDismissRequestOnline()
                onFailureTrx(getPosResult(it.data.error, it.data.msjError))
            }
            is Results.Failure -> {
                onFailureTrx(PosResult.SyncOperationFailed)
            }
        }
    }
}