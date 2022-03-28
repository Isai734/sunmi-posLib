package com.pagatodo.sunmi.poslib.view.dialogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.FragmentMsiDialogBinding
import com.pagatodo.sunmi.poslib.model.Msi
import com.pagatodo.sunmi.poslib.view.adapter.MsiAdapter
import java.math.RoundingMode
import java.text.DecimalFormat

class MsiDialog: DialogFragment(R.layout.fragment_msi_dialog) {

    private lateinit var binding: FragmentMsiDialogBinding
    private val singlePayment: Int = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMsiDialogBinding.bind(view)
        val args = this.arguments
        val inputDataAmount = args?.get("amount")

        binding.tvAmount.text = inputDataAmount.toString()
        val amount1: String = inputDataAmount.toString()
        val amount: Double = amount1.toDouble()

        val firstArray = arrayOf(3,6,9,12,18,24)
        val finalListMonth = generateMonthList(firstArray,amount)

        binding.recycler.adapter = MsiAdapter(
            finalListMonth
        ) {month ->
            Toast.makeText(context, "${month.monthNumber}", Toast.LENGTH_SHORT).show()
            binding.recycler.adapter?.notifyDataSetChanged()
        }

        binding.singlePayment.setOnClickListener {
            Toast.makeText(context,"$singlePayment",Toast.LENGTH_SHORT).show()
        }

        binding.closeDialog.setOnClickListener {
            dismiss()
        }

    }

    private fun generateMonthList(arrayMonth: Array<Int>, amount: Double): List<Msi> {
        var arraySize = arrayMonth.size
        val listMonth = mutableListOf<Msi>()
        for(i in 0 until arraySize) {
            var  decimalRoundingFormat = DecimalFormat("#")
            decimalRoundingFormat.roundingMode = RoundingMode.CEILING

            var finalAmount =  (amount/arrayMonth[i])
            listMonth.add(i,Msi(arrayMonth[i],decimalRoundingFormat.format(finalAmount)))
        }
        return listMonth
    }
}