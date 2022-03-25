package com.pagatodo.sunmi.poslib.util

import com.pagatodo.sunmi.poslib.view.BigDecimalAdapter
import com.pagatodo.sunmi.poslib.view.DateAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object MoshiInstance {
    fun create(): Moshi = Moshi.Builder()
        .add(BigDecimalAdapter)
        .add(DateAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
}