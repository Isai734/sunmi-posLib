package com.pagatodo.sunmi.poslib.model

import net.fullcarga.android.api.data.DataOpTarjeta

data class SyncData(
    val product: String,
    val params: List<String>,
    val dataCard: DataOpTarjeta,
    val stan: Long
)
