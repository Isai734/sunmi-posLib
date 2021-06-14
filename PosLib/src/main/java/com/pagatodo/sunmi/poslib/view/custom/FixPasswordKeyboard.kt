package com.pagatodo.sunmi.poslib.view.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.pagatodo.sunmi.poslib.R

class FixPasswordKeyboard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {
    lateinit var key0: TextView
    private lateinit var key1: TextView
    private lateinit var key2: TextView
    private lateinit var key3: TextView
    private lateinit var key4: TextView
    private lateinit var key5: TextView
    private lateinit var key6: TextView
    private lateinit var key7: TextView
    private lateinit var key8: TextView
    private lateinit var key9: TextView
    private fun initView(context: Context) {
        inflate(context, R.layout.view_fix_password_keyboard, this)
        key0 = findViewById(R.id.text_0)
        key1 = findViewById(R.id.text_1)
        key2 = findViewById(R.id.text_2)
        key3 = findViewById(R.id.text_3)
        key4 = findViewById(R.id.text_4)
        key5 = findViewById(R.id.text_5)
        key6 = findViewById(R.id.text_6)
        key7 = findViewById(R.id.text_7)
        key8 = findViewById(R.id.text_8)
        key9 = findViewById(R.id.text_9)
    }

    fun setKeyBoard(keys: String?) {
        if (keys == null || keys.length != 10) return
        var temp = keys.substring(0, 1)
        key0.text = temp
        temp = keys.substring(1, 2)
        key1.text = temp
        temp = keys.substring(2, 3)
        key2.text = temp
        temp = keys.substring(3, 4)
        key3.text = temp
        temp = keys.substring(4, 5)
        key4.text = temp
        temp = keys.substring(5, 6)
        key5.text = temp
        temp = keys.substring(6, 7)
        key6.text = temp
        temp = keys.substring(7, 8)
        key7.text = temp
        temp = keys.substring(8, 9)
        key8.text = temp
        temp = keys.substring(9, 10)
        key9.text = temp
    }

    init {
        initView(context)
    }
}