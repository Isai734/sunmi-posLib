package com.pagatodo.sunmi.poslib.model

import com.squareup.moshi.JsonClass
import net.fullcarga.android.api.data.DataOpTarjeta

@JsonClass(generateAdapter = true)
data class SyncData(
    val product: String,
    val params: List<String>,
    val dataCard: DataOpTarjeta,
    val stan: Long
)
