package com.pagatodo.sunmi.poslib.view.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.pagatodo.sunmi.poslib.R

class DialogProgress(context: Context) :
    Dialog(context, R.style.AppTheme_AppCompat_Dialog_Alert_NoFloating) {

    private var title = "Enviando..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress_show)
    }

    override fun onStart() {
        super.onStart()
        //window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun show() {
        super.show()
        findViewById<TextView>(R.id.titleProgressDialog).text = title
    }

    fun setTitle(title: String) {
        this.title = title
    }

    init {
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun hide() {
        this.dismiss()
    }
}