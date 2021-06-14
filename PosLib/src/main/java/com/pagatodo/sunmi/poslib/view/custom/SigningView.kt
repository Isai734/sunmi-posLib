package com.pagatodo.sunmi.poslib.view.custom

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class SigningView(context: Context?) : View(context) {
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPaint: Paint
    var path = Path()
    var sign: Signature
        private set
    var previousX = 0f
    var previousY = 0f
    fun clear() {
        if (mCanvas != null) {
            mCanvas!!.drawColor(Color.WHITE)
            invalidate()
            sign = Signature()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        var curW = if (mBitmap != null) mBitmap!!.width else 0
        var curH = if (mBitmap != null) mBitmap!!.height else 0
        if (curW >= width && curH >= height) return
        if (curW < width) curW = width
        if (curH < height) curH = height
        mBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.RGB_565)
        mCanvas = Canvas(mBitmap!!)
        mCanvas!!.drawColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap!!, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointX = event.x
        val pointY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(pointX, pointY)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(pointX, pointY)
                invalidate()
            }
            else -> {
            }
        }
        return true
    }

    private fun touchStart(pointX: Float, pointY: Float) {
        path.reset()
        path.moveTo(pointX, pointY)
        previousX = pointX
        previousY = pointY
        sign.newStroke()
    }

    private fun touchMove(pointX: Float, pointY: Float) {
        val distanceX = Math.abs(pointX - previousX)
        val distanceY = Math.abs(pointY - previousY)
        if (distanceX >= TOUCH_TOLERANCE || distanceY >= TOUCH_TOLERANCE) {
            path.quadTo(previousX, previousY, (pointX + previousX) / 2, (pointY + previousY) / 2)
            mCanvas!!.drawPath(path, mPaint)
            previousX = pointX
            previousY = pointY
        }
    }

    companion object {
        private const val TOUCH_TOLERANCE = 2f
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.alpha = 100
        mPaint.strokeWidth = 5f
        mPaint.setARGB(0xff, 0x33, 0x33, 0x33)
        sign = Signature()
    }
}