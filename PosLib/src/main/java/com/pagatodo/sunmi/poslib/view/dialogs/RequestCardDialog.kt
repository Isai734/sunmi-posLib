package com.pagatodo.sunmi.poslib.view.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.pagatodo.sigmalib.ApiData
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.RequestCardDialogBinding
import com.pagatodo.sunmi.poslib.util.AmountUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RequestCardDialog(context: Context) : AlertDialog(context) {
    private lateinit var binding : RequestCardDialogBinding
    var mensaje: String? = null
    var amount: String? = null
    var dismissable = true
    var showRfReading = false

    @SuppressLint("ClickableViewAccessibility")
    override fun show() {
        super.show()
        binding = RequestCardDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.titleRequestCard.text = mensaje
        val nAmt =  amount?.let {ApiData.APIDATA.datosSesionPCI.datosTPV.convertirImporte(it)} ?: BigDecimal.ZERO
        binding.amountRequestCard.setText(AmountUtils.formatAmount(nAmt, 2), TextView.BufferType.SPANNABLE)
        binding.requestCardAnim.playAnimation()
        if (!showRfReading) {
            binding.contentLeds.visibility = View.GONE
        }
    }

    fun setMessage(mensaje: String?) {
        GlobalScope.launch(Dispatchers.Main) { binding.titleRequestCard.text = mensaje }
    }

    fun reading() {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                binding.amountRequestCard.visibility = View.GONE
                binding.contentLeds.visibility = View.VISIBLE
                dismissable = false
                ledStatusOnDevice(status01 = true, status02 = false, status03 = false, status04 = false)
                delay(68)
                ledStatusOnDevice(status01 = true, status02 = true, status03 = false, status04 = false)
                delay(68)
                ledStatusOnDevice(status01 = true, status02 = true, status03 = true, status04 = false)
                delay(68)
                ledStatusOnDevice(status01 = true, status02 = true, status03 = true, status04 = true)
                binding.titleRequestCard.text = "Retire la tarjeta"
                delay(750)
                dismissable =  true
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("VisibilityUtils", "result:$e")
            }
        }
    }

    private fun ledStatusOnDevice(status01: Boolean, status02: Boolean, status03: Boolean, status04: Boolean) {
        binding.led01.isChecked = status01
        binding.led02.isChecked = status02
        binding.led03.isChecked = status03
        binding.led04.isChecked = status04
    }

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}