package com.pagatodo.sunmi.poslibimpl

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.SunmiTrxWrapper
import com.pagatodo.sunmi.poslib.config.PinPadConfigV3
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.*
import com.pagatodo.sunmi.poslib.util.*
import com.pagatodo.sunmi.poslibimpl.databinding.ActivityMainBinding
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.Respuesta

class MainActivity : AppCompatActivity(), SunmiTrxListener<String> {

    private lateinit var binding : ActivityMainBinding
    private val viewMPci by lazy { ViewModelProvider(this)[ViewModelPci::class.java] }
    private val trxManager by lazy { SunmiTrxWrapper(this, test = true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        PosLib.createInstance(this)
        binding.btnAccept.setOnClickListener {
            if (binding.amountTxv.text.isNotEmpty() && binding.amountTxv.text.isDigitsOnly())
                trxManager.initTransaction()
        }
    }

    override fun onStart() {
        super.onStart()
        initTerminal()
    }

    private fun initTerminal() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                delay(2000L)
                val fileAid = resources.openRawResource(R.raw.aids_es_1_2)
                val fileCapk = resources.openRawResource(R.raw.capks_es_1_2)
                val fileDrl = resources.openRawResource(R.raw.dlrs_es_1_0)
                val aidList = LoadFile.readConfigFile<Aid>(fileAid)
                val capkList = LoadFile.readConfigFile<Capk>(fileCapk)
                val drlsList = LoadFile.readConfigFile<Drl>(fileDrl)
                val posConfig = PosConfig()
                posConfig.aids = aidList
                posConfig.capks = capkList
                posConfig.drls = drlsList
                PosLib.loadGlobalConfig(posConfig)
                Log.i("MainActivity", "configure terminal success")
            } catch (e: Exception) {
                PosLogger.e("MainActivity", e.toString())
            }
        }
    }

    override fun onDialogRequestCard(message: String?, cardTypes: Int) {
        askForCard?.show()
    }

    override fun onDismissRequestCard() {
        askForCard?.dismiss()
    }

    override fun onDialogProcessOnline(message: String?, dataCard: DataCard?) {
        dialogProgress.show(supportFragmentManager, dialogProgress.tag)
    }

    override fun onDismissRequestOnline() {
        if (dialogProgress.isAdded) dialogProgress.dismiss()
    }

    override fun onShowSingDialog(doContinue: (ByteArray) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Mostrar dialogo de firma", Toast.LENGTH_SHORT).show()
            delay(1000L)
            doContinue(ByteArray(0))
        }
    }

    override fun createTransactionData() = TransactionData().apply {
        transType = Constants.TransType.PURCHASE
        amount = binding.amountTxv.text.toString()
        totalAmount = binding.amountTxv.text.toString()
        otherAmount = "00"
        currencyCode = "0156"
        cashBackAmount = "00"
        taxes = "00"
        comisions = "00"
        gratuity = "00"
        sigmaOperation = "V"
        tagsEmv = EmvUtil.tagsDefault.toList()
        terminalParams = EmvTermParamV2()
    }

    override fun pinMustBeForced(): Boolean {
        return false
    }

    override fun checkCardTypes(): Int {
        return AidlConstants.CardType.MAGNETIC.value or AidlConstants.CardType.IC.value or AidlConstants.CardType.NFC.value
    }

    override fun onShowTicketDialog(responseTrx: Respuesta?, dataCard: DataCard, singBytes: ByteArray?) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Mostrar dialogo de ticket", Toast.LENGTH_LONG).show()
        }
    }

    override fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3) {
        val pinPadDialog = PinPadDialog.createInstance(pinPadConfig)
        pinPadDialog.setPasswordLength(6)
        pinPadDialog.setTextAccept("Aceptar")
        pinPadDialog.setTextCancel("Cancelar")
        pinPadDialog.setPinPadListenerV2(pinPadListener)

        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.add(android.R.id.content, pinPadDialog, pinPadDialog.tag).commit()
    }

    override fun onShowSelectApp(listEmvApps: List<String>, appSelect: (Int) -> Unit) {
        appSelect(0)
    }

    override fun onSync(dataCard: DataCard) {
        viewMPci.sync()
    }

    override fun onFailureEmv(error: PosResult, todo: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPurchase(dataCard: DataCard) {
        viewMPci.purchase()
    }

    override fun doOperationNext(nextOperation: OperacionSiguiente, message: String, doContinue: (String) -> Unit) {
        doContinue(message)
        Toast.makeText(this, "Operacion Siguiente", Toast.LENGTH_LONG).show()
    }

    override fun getVmodelPCI() = viewMPci

    private val dialogProgress: DialogProgress by lazy {
        DialogProgress().apply { isCancelable = false }
    }

    private val askForCard: AlertDialog? by lazy {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Por favor inserta, desliza o acerca la tarjeta.")
        builder.create()
    }

    override fun onShowDniDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    override fun onShowZipDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    override fun showReading() {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Leyendo tarjeta", Toast.LENGTH_SHORT).show()
        }
    }

    override fun showRemoveCard(dataCard: DataCard?) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Por favor retire la tarjeta.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun isPossibleFallback(): Boolean = true

    override fun requireSignature(dataCard: DataCard) = false

    override fun onFailureOnline(error: PosResult, doContinue: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "onFailureOnline.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSuccessOnline(doContinue: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "onSuccessOnline.", Toast.LENGTH_SHORT).show()
            delay(2000L)
            doContinue()
        }
    }
}