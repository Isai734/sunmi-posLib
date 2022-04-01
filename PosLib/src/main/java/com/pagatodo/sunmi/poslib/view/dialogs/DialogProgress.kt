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
    private lateinit var viewTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress_show)
    }

    override fun show() {
        super.show()
        viewTitle = findViewById(R.id.titleProgressDialog)
        viewTitle.text = title
    }

    fun setTitle(title: String) {
        if(this::viewTitle.isInitialized)
            viewTitle.text = title
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