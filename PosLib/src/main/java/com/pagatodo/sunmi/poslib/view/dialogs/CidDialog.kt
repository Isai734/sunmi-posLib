package com.pagatodo.sunmi.poslib.view.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import com.pagatodo.sigmalib.EmvManager
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.CidDialogBinding
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.EmvRangoBinCuotas
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.PerfilesEmv
import java.util.*
import kotlin.collections.ArrayList

class CidDialog : DialogFragment() {
    private lateinit var binding: CidDialogBinding
    private lateinit var okListener: View.OnClickListener
    private lateinit var cancelListener: View.OnClickListener

    fun setOkListener(cuotasListener: View.OnClickListener) {
        this.okListener = cuotasListener
    }

    fun setCancelListener(cancelListener: View.OnClickListener) {
        this.cancelListener = cancelListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FillHorizontalDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CidDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.okButton.setOnClickListener {
            if(isValid()){
                dismiss()
                it.tag = binding.editText.text.toString()
                okListener.onClick(it)
            }
        }

        binding.btnCancelarCustom.setOnClickListener {
            dismiss()
            cancelListener.onClick(it)
        }
    }

    private fun isValid(): Boolean{
        return  binding.editText.text.isNotEmpty() && binding.editText.text.isDigitsOnly()
    }

    companion object {
        fun newInstance(): CidDialog {
            val args = Bundle()
            val fragment = CidDialog()
            fragment.arguments = args
            return fragment
        }
    }
}