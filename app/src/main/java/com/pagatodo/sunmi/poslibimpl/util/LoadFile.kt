package com.pagatodo.sunmi.poslibimpl.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.util.*

object LoadFile {

    inline fun <reified E> readConfigFile(file: InputStream): List<E> {//Convert hex string to Aids
        val reader = BufferedReader(InputStreamReader(file))
        reader.readText().apply {
            val moshi = Moshi.Builder().build()
            val type: Type = Types.newParameterizedType(MutableList::class.java, E::class.java)
            val adapter: JsonAdapter<List<E>> = moshi.adapter(type)
            return adapter.fromJson(this) ?: LinkedList()
        }
    }
}