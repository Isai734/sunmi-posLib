package com.pagatodo.sunmi.poslib.view.dialogs

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.view.custom.SigningView
import com.watermark.androidwm_light.WatermarkBuilder
import com.watermark.androidwm_light.bean.WatermarkText
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SingDialog : DialogFragment(), View.OnClickListener {
    private lateinit var signingView: SigningView
    private var signListe: SignatureListener? = null
    private var refLocal: String? = null

    interface SignatureListener {
        fun onSingSuccess(singBytes: ByteArray)
        fun onCancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vista: View = inflater.inflate(R.layout.sign_dialog, container, false)
        asociarLayout(vista)
        return vista
    }

    private fun asociarLayout(vista: View) {
        val rlPanelFirma: RelativeLayout = vista.findViewById(R.id.firma_panel_firma)
        vista.findViewById<View>(R.id.btn_limpiar_firma).setOnClickListener(this)
        vista.findViewById<View>(R.id.btn_finalizar).setOnClickListener(this)
        signingView = SigningView(activity)
        signingView.isDrawingCacheEnabled = true
        signingView.buildDrawingCache()
        rlPanelFirma.addView(signingView)
    }

    private fun setSignListe(signListe: SignatureListener) {
        this.signListe = signListe
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_limpiar_firma -> signingView.clear()
            R.id.btn_finalizar -> {
                sendFirmaResult()
                dismiss()
            }
            else -> {
            }
        }
    }

    private fun sendFirmaResult() {
        val bmp: Bitmap = signingView.drawingCache
        if (signingView.sign.numberStrokes > 0) {
            if (signListe != null) {
                signListe!!.onSingSuccess(getByteArray(shrinkBitmap(bmp)))
            }
        } else {
            if (signListe != null) {
                signListe!!.onCancel()
            }
        }
    }

    private fun shrinkBitmap(myBitmap: Bitmap): Bitmap {
        val maxSize = 500 //Max Size
        val outWidth: Int
        val outHeight: Int
        val inWidth: Int = myBitmap.width
        val inHeight: Int = myBitmap.height
        if (inWidth > inHeight) {
            outWidth = maxSize
            outHeight = inHeight * maxSize / inWidth
        } else {
            outHeight = maxSize
            outWidth = inWidth * maxSize / inHeight
        }
        val matrix = Matrix()
        matrix.postRotate(0f)
        val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(myBitmap, outWidth, outHeight, false)
        val waterMark = setWatermark(scaledBitmap)
        return Bitmap.createBitmap(waterMark, 0, 0, waterMark.width, waterMark.height, matrix, true)
    }

    private fun getByteArray(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        return bos.toByteArray()
    }

    private fun setWatermark(src: Bitmap): Bitmap {
        val sdf = SimpleDateFormat("dd/MMM/yyyy hh:mm:ss", Locale.getDefault())
        val fecha = sdf.format(Date())
        return WatermarkBuilder.create(this.context, src).loadWatermarkText(WatermarkText("$fecha $refLocal - ").setTextSize(14.0).setTextColor(Color.BLACK).setTextAlpha(150)).setTileMode(true).watermark.outputImage
    }

    companion object {
        fun newInstance(listener: SignatureListener, refLocal: String): SingDialog {
            val firmaDialogo = SingDialog()
            firmaDialogo.refLocal = refLocal
            firmaDialogo.setSignListe(listener)
            return firmaDialogo
        }
    }
}