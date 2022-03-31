package com.pagatodo.sunmi.poslib.view.dialogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.databinding.FragmentMsiDialogBinding
import com.pagatodo.sunmi.poslib.model.Msi
import com.pagatodo.sunmi.poslib.view.adapter.MsiAdapter
import java.math.BigDecimal
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
        val inputMsiList = args?.get("msilist")

        binding.tvAmount.text = inputDataAmount.toString()
        val amount1: String = inputDataAmount.toString()
        val amount: Double = amount1.toDouble()

        //val firstArray = arrayOf(3,6,9,12,18,24,32)
        val finalListMonth = generateMonthList(inputMsiList as List<Int>,amount)

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

    private fun generateMonthList(msiList: List<Int>, amount: Double): List<Msi> {
        var arraySize = msiList.size
        val listMonth = mutableListOf<Msi>()
        for(i in 0 until arraySize) {
            var  decimalRoundingFormat = DecimalFormat("#")
            decimalRoundingFormat.roundingMode = RoundingMode.CEILING

            var finalAmount =  (amount/msiList[i])
            listMonth.add(i,Msi(msiList[i],decimalRoundingFormat.format(finalAmount)))
        }
        return listMonth
    }

    companion object{
        fun create(totalAmount: String, msiList: List<Int>,doContinue: (Boolean) -> Unit): MsiDialog{
            val args = Bundle()
            args.putString("amount",totalAmount)
            args.putStringArray("msilist",msiList.map { it.toString() }.toTypedArray())

            var msiDialog = MsiDialog()
            msiDialog.arguments = args
            return msiDialog
            //msiDialog.show(supportFragmentManager,"msiDialog")
        }
    }

}