package com.pagatodo.sunmi.poslibimpl

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pagatodo.sunmi.poslib.SunmiTransaction
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.util.Constants
import com.pagatodo.sunmi.poslib.util.EmvUtil
import com.pagatodo.sunmi.poslib.util.PosResult
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SunmiTransactionManager(private val activity: AppCompatActivity) : SunmiTransaction(),
    Observer<Results<String>> {

    var dialogProgress: DialogProgress? = null

    private val viewModelPci: ViewModelPci by lazy {
        ViewModelProvider(activity)[ViewModelPci::class.java].apply {
            purchaseMlData.observe(activity, this@SunmiTransactionManager)
        }
    }
    private val mTransactionData = TransactionData()

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
        showDialog()
        super.startPayProcess()
    }

    private fun showDialog() {
        dialogProgress = DialogProgress()
        dialogProgress?.isCancelable = false
        dialogProgress?.show(activity.supportFragmentManager, dialogProgress?.tag)
    }

    override fun goOnlineProcess(dataCard: DataCard) {
        viewModelPci.purchase()
    }

    override fun onFailureTrx(result: PosResult) {
        dialogProgress?.dismiss()
        Toast.makeText(activity, "${result.code} ${result.message}", Toast.LENGTH_LONG).show()
    }

    override fun onApprovedTrx() {
        dialogProgress?.dismiss()
        Toast.makeText(activity, "Operación aprovada", Toast.LENGTH_LONG).show()
    }

    override fun getCheckCardType(): Int {
        return AidlConstants.CardType.MAGNETIC.value// or AidlConstants.CardType.IC.value or AidlConstants.CardType.NFC.value
    }

    override fun pinMustBeForced(): Boolean {
        return false
    }

    override fun onShowPinPad(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2) {
        val pinPadDialog = PinPadDialog.createInstance(pinPadConfig)
        pinPadDialog.setPasswordLength(6)
        pinPadDialog.setTextAccept("Aceptar")
        pinPadDialog.setTextCancel("Cancelar")
        pinPadDialog.setPinPadListenerV2(pinPadListener)
        val transaction: FragmentTransaction = activity.supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.add(android.R.id.content, pinPadDialog, pinPadDialog.tag).commit()
    }

    override fun onSelectEmvApp(listEMVApps: List<String>, applicationEmv: AppEmvSelectListener) {
        Toast.makeText(activity, "$listEMVApps", Toast.LENGTH_LONG).show()
        if (listEMVApps.isNotEmpty()) applicationEmv.onAppEmvSelected(
            listEMVApps.withIndex().first().index
        )
    }

    override fun getTransactionData() = mTransactionData

    override fun onChanged(t: Results<String>?) {
        when (t) {
            is Results.Success -> {
                finishOnlineProcessStatus(tlvResponse = Constants.TlvResponses.Approved)
            }
            is Results.Failure -> {
                finishOnlineProcessStatus(
                    tlvResponse = Constants.TlvResponses.Decline,
                    message = t.exception.message
                )
            }
        }
    }
}