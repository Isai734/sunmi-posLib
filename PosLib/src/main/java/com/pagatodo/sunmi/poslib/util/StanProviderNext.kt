package com.pagatodo.sunmi.poslib.util

import net.fullcarga.android.api.data.StanProvider

abstract class StanProviderNext: StanProvider {

    var onSaveFromLong: OnSaveFromLong? = null

    fun setCallback(onSaveFromLong: OnSaveFromLong){
        this.onSaveFromLong = onSaveFromLong
    }

    fun onCalculateNext(stan: Long) {
        onSaveFromLong?.onSaveWithStan(stan)
    }
}

interface OnSaveFromLong {
    fun onSaveWithStan(stan: Long)
}