package com.pagatodo.sunmi.poslib.view.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.DialogSelectEmvappBinding
import com.pagatodo.sunmi.poslib.databinding.ItemDatosAgenteBinding

class SelectEmvAppDialog : DialogFragment() {
    private lateinit var binding: DialogSelectEmvappBinding
    private lateinit var aplicacionesList: List<String>
    private var position = -1
    private lateinit var appSelect: (Int) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FillHorizontalDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSelectEmvappBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.AnimacionModal
        isCancelable = false
    }

    fun setAplicaciones(aplicacionesList: List<String>, appSelect: (Int) -> Unit) {
        this.aplicacionesList = aplicacionesList
        this.appSelect = appSelect
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTituloModal.text = getString(R.string.seleccion_operacion)
        val adapter = StringAdapter(aplicacionesList)
        val llm = LinearLayoutManager(requireContext())
        binding.rvOperaciones.layoutManager = llm
        binding.rvOperaciones.adapter = adapter
        binding.btnAceptEmvappp.setOnClickListener {
            if(position != -1) {
                dismiss()
                appSelect(position)
            }
        }
    }

    inner class StringAdapter internal constructor(var apps: List<String>) : RecyclerView.Adapter<StringAdapter.Holder>() {
        private lateinit var innerBinding: ItemDatosAgenteBinding
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_datos_agente, parent, false)
            innerBinding = ItemDatosAgenteBinding.bind(view)
            return Holder(innerBinding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.binding.tvAgente.text = apps[position]
            if(this@SelectEmvAppDialog.position == position){
                holder.binding.root.setBackgroundResource(R.drawable.lb_bg_button_accept)
            }else {
                holder.binding.root.setBackgroundResource(R.drawable.lb_bg_item_no_selected)
            }
            holder.binding.root.setOnClickListener {
                this@SelectEmvAppDialog.position = position
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        inner class Holder(val binding: ItemDatosAgenteBinding) : RecyclerView.ViewHolder(binding.root)
    }
}