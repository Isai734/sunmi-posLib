package com.pagatodo.sunmi.poslib.view.custom

import java.io.Serializable
import java.util.*

class Signature : Serializable {
    private val signatureStrokes: MutableList<Int> = ArrayList()
    fun newStroke() {
        val currentSignStroke = 1
        signatureStrokes.add(currentSignStroke)
    }

    val numberStrokes: Int
        get() = signatureStrokes.size

    companion object {
        private const val serialVersionUID = -7332384231293918281L
    }

    init {
        signatureStrokes.clear()
    }
}