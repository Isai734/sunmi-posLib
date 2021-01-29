package com.pagatodo.sunmi.poslib.interfaces

import android.view.View

interface OnClickAcceptListener : View.OnClickListener {
    fun onClickAccept(view: View?)
    override fun onClick(p0: View?) {
        onClickAccept(p0)
    }
}