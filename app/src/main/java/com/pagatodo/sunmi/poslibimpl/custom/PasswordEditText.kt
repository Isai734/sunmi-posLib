package com.pagatodo.sunmi.poslibimpl.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class PasswordEditText constructor(context: Context,attributeSet: AttributeSet):LinearLayout(context,attributeSet) {
    private var paint= Paint()
    private var paintVertical=Paint()

    private var maxDigit = 12
    private var numberDigits = 6
    private var inputSB: StringBuilder? = null
    private var  listText: ArrayList<TextView> = arrayListOf()
    var text=""
    set(value) {
        field = value
        if(value.isEmpty()){
            listText.clear()
            numberDigits =6
            repaintDigit()
            return
        }
        field.forEachIndexed { index, c ->
            if(listText.size <= index){
                return@forEachIndexed
            }
            listText[index].text = c.toString()
        }
    }

    init {
        init()
    }
    
    fun repaintDigit(){
        var r = numberDigits-listText.count()
        r = abs(r- maxDigit )
        removeAllViews()
        if(r>=0){
            for (i in 0 until r) {
                listText.add(TextView(context).apply {
                    layoutParams = LayoutParams((width / numberDigits)-3, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    textAlignment = TEXT_ALIGNMENT_CENTER
                    visibility = View.VISIBLE
                    id = generateViewId()
                    setBackgroundColor(Color.TRANSPARENT)
                    setTextColor(Color.parseColor("#212121"))
                    setHintTextColor(Color.parseColor("#b2b2b2"))
                    gravity = Gravity.START
                    textSize = 18f
                })
            }
        }
        listText.forEachIndexed {index,view->
            view.layoutParams = LayoutParams((width / numberDigits)-3, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            addView(view,index)
        }
    }

    private fun init() {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.parseColor("#00a0df")
        paintVertical.style = Paint.Style.STROKE
        paintVertical.strokeWidth = 1f
        paintVertical.color = Color.parseColor("#f3f4f5")
        orientation = HORIZONTAL
        gravity = Gravity.START
        inputSB = StringBuilder()
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        background = null
        canvas.drawLine(0f, this.height - 10.toFloat(), this.width - 1.toFloat(), this.height - 10.toFloat(), paint)
        val xi = (this.measuredWidth /numberDigits)
        for (i in 1 until numberDigits) {
            canvas.drawLine(xi * i.toFloat(), 5f, xi * i.toFloat(), this.height - 15.toFloat(), paintVertical)
        }
    }

    fun setNumberDigits(passwordLength: Int) {
        this.numberDigits = passwordLength
    }
    
}