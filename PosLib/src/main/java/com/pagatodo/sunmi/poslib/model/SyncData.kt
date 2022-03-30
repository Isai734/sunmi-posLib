package com.pagatodo.sunmi.poslib.model

import com.squareup.moshi.JsonClass
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Menu
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Operaciones

@JsonClass(generateAdapter = true)
data class SyncData(
    val product: String,
    val menu: Menu,
    val params: List<String>,
    val dataCard: DataCard,
    val transactionData: TransactionData,
    var stan: Long,
    val operation: Operaciones
)
