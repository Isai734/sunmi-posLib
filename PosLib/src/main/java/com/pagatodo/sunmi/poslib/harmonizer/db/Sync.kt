package com.pagatodo.sunmi.poslib.harmonizer.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*


@Entity(tableName = "Sync")
class Sync(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var data: String? = null,
    var status: String? = null,
    var dateTime: Date? = null
)