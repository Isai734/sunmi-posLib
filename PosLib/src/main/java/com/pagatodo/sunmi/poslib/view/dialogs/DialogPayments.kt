package com.pagatodo.sunmi.poslib.view.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.pagatodo.sigmalib.EmvManager
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.DialogCuotasSelectBinding
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.EmvRangoBinCuotas
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.PerfilesEmv
import java.util.*
import kotlin.collections.ArrayList

class DialogPayments : DialogFragment() {
    private lateinit var binding: DialogCuotasSelectBinding
    private lateinit var cuotasListener: View.OnClickListener
    private lateinit var cancelListener: View.OnClickListener
    private val listMonths = ArrayList<String>()
    private val perfilesEmv: PerfilesEmv? by lazy { arguments?.getSerializable(PARAM_PERFIL) as PerfilesEmv }

    fun setCuotasListener(cuotasListener: View.OnClickListener) {
        this.cuotasListener = cuotasListener
    }

    fun setCancelListener(cancelListener: View.OnClickListener) {
        this.cancelListener = cancelListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FillHorizontalDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogCuotasSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.bntAceptCuota.setOnClickListener {
            dismiss()
            cuotasListener.onClick(it.apply { tag = listMonths[binding.pickerQuotas.value].toInt()})
        }
        binding.bntCanceltCuota.setOnClickListener {
            dismiss()
            cancelListener.onClick(it)
        }


        perfilesEmv?.apply {
            val quotesMonth = EmvManager.getRangoCuotas(this.lstCuotasMes).let {
                LinkedList<EmvRangoBinCuotas>().apply {
                    for(quot in it){
                        if(quot.cuotasmax > 0 && quot.cuotasinc > 0)
                            add(quot)
                    }
                }
            }
            var min = 0
            var max = 0
            var inc = 0
            for (rangeQuotes in quotesMonth) {
                min = rangeQuotes.cuotasmin
                max = rangeQuotes.cuotasmax
                inc = rangeQuotes.cuotasinc
            }
            listMonths.add("0")// 0 or 1 ??
            for (mo in min..max step inc)
                listMonths.add("$mo")
            binding.pickerQuotas.displayedValues = listMonths.toTypedArray()

            binding.pickerQuotas.minValue = 0
            binding.pickerQuotas.maxValue = listMonths.size -1
        }
    }

    companion object {
        const val PARAM_PERFIL = "DialogCuotas.PARAM_PERFIL"
        fun newInstance(perfilesEmv: PerfilesEmv): DialogPayments {
            val args = Bundle()
            args.putSerializable(PARAM_PERFIL, perfilesEmv)
            val fragment = DialogPayments()
            fragment.arguments = args
            return fragment
        }
    }
}