package com.pagatodo.sunmi.poslib.util

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.SuperscriptSpan
import com.pagatodo.sigmalib.SigmaBdManager
import com.pagatodo.sigmalib.listeners.OnFailureListener
import com.pagatodo.sunmi.poslib.PosLib
import java.math.BigDecimal
import java.text.NumberFormat

object AmountUtils {

    val numberFormat: NumberFormat
        get() = SigmaBdManager.getNumberFormat(OnFailureListener.BasicOnFailureListener(PosLib.TAG, "Error al obtener formato."))

    fun formatAmount(amount: BigDecimal, decimals: Int, currency: String = ""): SpannableString {//With currency
        val nAmount = "$currency ${numberFormat.format(amount)}"
        val spannableString = SpannableString(nAmount)
        //spannableString.setSpan(TopAlignSuperscriptSpan(0.5.toFloat()), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(TopAlignSuperscriptSpan(0.15.toFloat()), nAmount.length - (decimals + 1), nAmount.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

    private class TopAlignSuperscriptSpan//sets the shift percentage
        (shiftPercentage: Float) : SuperscriptSpan() {
        //divide superscript by this number
        private var fontScale = 2

        //shift value, 0 to 1.0
        private var shiftPercentage = 0f

        init {
            if (shiftPercentage > 0.0 && shiftPercentage < 1.0) this.shiftPercentage = shiftPercentage
        }

        override fun updateDrawState(tp: TextPaint) { //original ascent
            val ascent = tp.ascent()
            tp.textSize = tp.textSize / fontScale
            val newAscent = tp.fontMetrics.ascent
            tp.baselineShift += (ascent - ascent * shiftPercentage - (newAscent - newAscent * shiftPercentage)).toInt()
        }

        override fun updateMeasureState(tp: TextPaint) {
            updateDrawState(tp)
        }
    }

    private fun isNumeric(caracter: Char): Boolean {
        return try {
            caracter.toString().toInt()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }


    private fun numbers(importe: String): String {
        val numbers = StringBuilder()
        for (element in importe) {
            val caracter = element
            if (isNumeric(caracter)) {
                numbers.append(caracter)
            }
        }
        return numbers.toString()
    }

    fun cleanImporteInput(importe: String, numberFormat: NumberFormat): BigDecimal {
        val cleanString: String = numbers(importe.replace(numberFormat.currency?.symbol?.toRegex()!!, "").replace(" ", ""))
        return if (cleanString.isNotEmpty()) {
            if (numberFormat.maximumFractionDigits != 0) {
                BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR)
                    .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)
            } else {
                BigDecimal(cleanString).setScale(0, BigDecimal.ROUND_DOWN)
            }
        } else BigDecimal.valueOf(0.00)
    }

}