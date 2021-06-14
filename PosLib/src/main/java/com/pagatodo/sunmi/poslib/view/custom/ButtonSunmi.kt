package com.pagatodo.sunmi.poslib.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.pagatodo.sunmi.poslib.R

class ButtonSunmi : ConstraintLayout {

    private var onClick: OnClickListener? = null
    private var mLastClickTime: Long = 0

    constructor(context: Context) : super(context) {
        init()
    }

    override fun setOnClickListener(onClick: OnClickListener?) {
        this.onClick = onClick
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    @SuppressLint("Recycle")
    private fun init(attrs: AttributeSet? = null) {
        inflate(context, R.layout.button_sunmi, this)
        attrs?.apply {
            val type: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ButtonSunmi)
            val text = type.getString(R.styleable.ButtonSunmi_text)
            val textView = findViewById<TextView>(R.id.textButton)
            text?.apply { textView.text = this }

            val drawable = type.getDrawable(R.styleable.ButtonSunmi_background)
            drawable?.apply { background = this }
        }
        super.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                return@setOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            onClick?.onClick(it)
        }
    }

    fun setText(text: String?) {
        val textView = findViewById<TextView>(R.id.textButton)
        textView.text = text
    }
}