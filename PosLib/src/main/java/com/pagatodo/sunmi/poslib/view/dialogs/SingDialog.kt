package com.pagatodo.sunmi.poslib.view.dialogs

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.view.custom.SigningView
import java.io.ByteArrayOutputStream

class SingDialog : DialogFragment(), View.OnClickListener {
    private lateinit var signingView: SigningView
    private var signListe: SignatureListener? = null

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
        val inWidth: Int = myBitmap.getWidth()
        val inHeight: Int = myBitmap.getHeight()
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
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true)
    }

    private fun getByteArray(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        return bos.toByteArray()
    }

    companion object {
        fun newInstance(listener: SignatureListener): SingDialog {
            val firmaDialogo = SingDialog()
            firmaDialogo.setSignListe(listener)
            return firmaDialogo
        }
    }
}