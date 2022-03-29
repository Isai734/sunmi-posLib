package com.pagatodo.sunmi.poslib.view.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.ViewMonthItemBinding
import com.pagatodo.sunmi.poslib.model.Msi
import kotlin.math.roundToInt

class MsiAdapter(
    private val months: List<Msi>,
    private val monthClickedListener: (Msi) -> Unit
) : RecyclerView.Adapter<MsiAdapter.ViewHolder>(){

    var previousItem = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewMonthItemBinding.inflate(LayoutInflater.from(parent.context), parent,false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val month = months[position]
        holder.bind(month)
        holder.itemView.setOnClickListener {
            previousItem = position
            monthClickedListener(month)
        }
        if (previousItem == position) {
            holder.itemView.setBackgroundResource(R.drawable.base_checked)
            holder.itemTextView?.setTextColor(Color.WHITE)
        }else {
            holder.itemView.setBackgroundResource(R.drawable.base_unchecked)
            holder.itemTextView?.setTextColor(Color.DKGRAY)

        }
    }

    override fun getItemCount() = months.size

    class ViewHolder(private val binding: ViewMonthItemBinding): RecyclerView.ViewHolder(binding.root){
        var itemTextView: TextView? = null

        fun bind(month : Msi) {
            val valueMulti = "${month.monthNumber} x ${month.amountMoney}"
            itemTextView = binding.monthButton
            binding.monthButton.text = valueMulti
        }
    }
}