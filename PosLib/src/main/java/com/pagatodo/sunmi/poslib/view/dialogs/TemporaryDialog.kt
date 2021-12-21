package com.pagatodo.sunmi.poslib.view.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.requireContext
import com.pagatodo.sunmi.poslib.util.PosResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TemporaryDialog private constructor(
    context: Context,
    val result: PosResult,
    val duration: Long? = SHORT_SHOW
) : Dialog(context) {

    private lateinit var doContinue: (String) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.temporary_dialog)
    }

    fun show(doContinue: (String) -> Unit) {
        this.doContinue = doContinue
        this.show()
    }

    override fun show() {
        super.show()
        val imageView = findViewById<ImageView>(R.id.imageVerifyCard)
        val title = findViewById<TextView>(R.id.titleTempDialog)
        val subTitle = findViewById<TextView>(R.id.subtTempDialog)
        title.text = result.tile
        result.message?.apply {
            subTitle.text = this
            title.textSize = spToPx(11f, requireContext())
        } ?: run { subTitle.visibility = View.GONE }

        imageView.setImageResource(
            when (result) {
                PosResult.CardPresentWait -> R.drawable.card_present
                PosResult.SeePhone -> R.drawable.see_phone
                PosResult.OnlineError -> R.drawable.ic_alerta_i
                PosResult.OnlineApproved -> R.drawable.icono_exitoso
                PosResult.InfoPinOk -> R.drawable.icono_exitoso
                else -> R.drawable.ic_icon_alerta_error
            }
        )

        duration?.apply {
            GlobalScope.launch(Dispatchers.Main) {
                delay(duration)
                dismiss()
                if (this@TemporaryDialog::doContinue.isInitialized) doContinue(result.tile)
            }
        }
    }

    companion object {
        fun create(context: Context, result: PosResult, duration: Long? = SHORT_SHOW) =
            TemporaryDialog(context, result, duration)

        const val SHORT_SHOW = 2000L
        const val MID_SHOW = 3000L
        const val LONG_SHOW = 4000L
    }


    init {
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun spToPx(sp: Float, context: Context): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    }

}