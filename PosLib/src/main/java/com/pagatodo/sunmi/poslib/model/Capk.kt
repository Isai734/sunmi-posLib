package com.pagatodo.sunmi.poslib.model

import com.pagatodo.sunmi.poslib.util.ByteUtil
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.sunmi.pay.hardware.aidlv2.bean.CapkV2

@JsonClass(generateAdapter = true)
data class Capk(

		@Json(name = "hashInd")
		val hashInd: String? = null,

		@Json(name = "length")
		val length: String? = null,

		@Json(name = "index")
		val index: String? = null,

		@Json(name = "checkSum")
		val checkSum: String? = null,

		@Json(name = "modul")
		val modul: String? = null,

		@Json(name = "rid")
		val rid: String? = null,

		@Json(name = "expDate")
		val expDate: String? = null,

		@Json(name = "exponent")
		val exponent: String? = null,

		@Json(name = "arithInd")
		val arithInd: String = "01"
) {
    fun toCapkV2(): CapkV2 {
        val capkV2 = CapkV2()
        hashInd?.apply { capkV2.hashInd = ByteUtil.hexStr2Byte(this) }
        index?.apply { capkV2.index = ByteUtil.hexStr2Byte(this) }
        checkSum?.apply { capkV2.checkSum = ByteUtil.hexStr2Bytes(this) }
        modul?.apply { capkV2.modul = ByteUtil.hexStr2Bytes(this) }
        rid?.apply { capkV2.rid = ByteUtil.hexStr2Bytes(this) }
        expDate?.apply { capkV2.expDate = ByteUtil.hexStr2Bytes(this) }
        exponent?.apply { capkV2.exponent = ByteUtil.hexStr2Bytes(this) }
        arithInd.apply { capkV2.arithInd = ByteUtil.hexStr2Byte(this) }
        return capkV2
    }
}
