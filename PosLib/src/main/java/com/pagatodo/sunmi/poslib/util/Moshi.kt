package com.pagatodo.sunmi.poslib.util

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.*

object MoshiInstance {

    val type: Type = Types.newParameterizedType(HashMap::class.java, String::class.java, String::class.java)

    fun create(): Moshi = Moshi.Builder()
        .add(BigDecimalAdapter)
        .add(DateAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
}

object BigDecimalAdapter {
    @FromJson
    fun fromJson(string: String) = BigDecimal(string)

    @ToJson
    fun toJson(value: BigDecimal) = value.toString()

}

object DateAdapter {

    @FromJson
    fun fromJson(date: String) = Date(date.toLong())

    @ToJson
    fun toJson(value: Date?) = value?.time.toString()
}