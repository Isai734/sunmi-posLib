package com.pagatodo.sunmi.poslib.model

import java.util.*

class TLV(tag: String?, val length: Int, value: String?) {
    val tag: String
    val value: String

    init {
        this.tag = null2UpperCaseString(tag)
        this.value = null2UpperCaseString(value)
    }

    fun recoverToHexStr(): String {
        return TLVUtil.revertToHexStr(this)
    }

    fun recoverToBytes(): ByteArray {
        return TLVUtil.revertToBytes(this)
    }

    override fun toString(): String {
        return "tag=[$tag],length=[$length],value=[$value]"
    }

    private fun null2UpperCaseString(src: String?): String {
        return src?.toUpperCase(Locale.ROOT) ?: ""
    }
}