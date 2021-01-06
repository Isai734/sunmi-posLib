package com.pagatodo.sunmi.poslibimpl

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentTransaction
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.Aid
import com.pagatodo.sunmi.poslib.model.Capk
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.PosLogger
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslibimpl.util.LoadFile
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity(),SunmiTrxListener {

    private val sunmiTransactionManager: SunmiTransactionManager by lazy {
        SunmiTransactionManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        PosLib.createInstance(this)
        btnAccept.setOnClickListener {
            if (amount.text.isNotEmpty() && amount.text.isDigitsOnly())
                sunmiTransactionManager.initTransaction(amount.text.toString())
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
                val aidList = LoadFile.readConfigFile<Aid>(fileAid)
                val capkList = LoadFile.readConfigFile<Capk>(fileCapk)
                val posConfig = PosConfig()
                posConfig.aids = aidList
                posConfig.capks = capkList
                PosLib.loadGlobalConfig(posConfig)
                Log.i("MainActivity", "configure terminal success")
            } catch (e: Exception) {
               PosLogger.e("MainActivity", e.toString())
            }
        }
    }

    override fun onShowRequestCard() {
        askForCard?.show()
    }

    override fun onDismissRequestCard() {
        TODO("Not yet implemented")
    }

    override fun onShowProcessOnline() {
        dialogProgress?.show(supportFragmentManager, dialogProgress?.tag)
    }

    override fun onDismissRequestOnline() {
        TODO("Not yet implemented")
    }

    override fun onShowSingDialog() {
        TODO("Not yet implemented")
    }

    override fun onShowDniDialog() {
        TODO("Not yet implemented")
    }

    override fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2) {
        val pinPadDialog = PinPadDialog.createInstance(pinPadConfig)
        pinPadDialog.setPasswordLength(6)
        pinPadDialog.setTextAccept("Aceptar")
        pinPadDialog.setTextCancel("Cancelar")
        pinPadDialog.setPinPadListenerV2(pinPadListener)
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.add(android.R.id.content, pinPadDialog, pinPadDialog.tag).commit()
    }

    override fun onShowSelectApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener) {
        TODO("Not yet implemented")
    }

    override fun <E> onSuccess(request: E) {

    }

    override fun onFailure(result: PosResult) {
        TODO("Not yet implemented")
    }

    private val dialogProgress: DialogProgress? by lazy {
        DialogProgress().apply {
            isCancelable = false
        }
    }

    private val askForCard: AlertDialog? by lazy {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Por favor inserta, desliza o acerca la tarjeta.")
        builder.create()
    }
}