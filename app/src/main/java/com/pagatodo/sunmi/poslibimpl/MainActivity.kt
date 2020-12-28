package com.pagatodo.sunmi.poslibimpl

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.text.isDigitsOnly
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.model.Aid
import com.pagatodo.sunmi.poslib.model.Capk
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslibimpl.util.LoadFile
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {

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
                Log.e("MainActivity", e.toString())
            }
        }
    }
}