package com.pagatodo.sunmi.poslib.model

data class Sync(
    val product: String,
    val params: List<String>,
    val dataCard: DataCard,
    val stan: Long
)
