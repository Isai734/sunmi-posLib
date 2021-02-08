package com.pagatodo.sunmi.poslib.model

import com.pagatodo.sunmi.poslib.util.ByteUtil
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.sunmi.pay.hardware.aidlv2.bean.DrlV2

@JsonClass(generateAdapter = true)
data class Drl(

    @Json(name = "isDefaultLmt")
    val isDefaultLmt: Boolean = false,

    @Json(name = "zeroCheck")
    val zeroCheck: Int = 1,

    @Json(name = "_comment")
    val comment: String? = null,

    @Json(name = "termClssLmt")
    val termClssLmt: String? = null,

    @Json(name = "termClssFloorLmt")
    val termClssFloorLmt: String? = null,

    @Json(name = "statusCheck")
    val statusCheck: Boolean = false,

    @Json(name = "cvmLmt")
    val cvmLmt: String? = null,

    @Json(name = "termFloorLmt")
    val termFloorLmt: String? = null,

    @Json(name = "cvmLmtActivate")
    val cvmLmtActivate: Boolean = true,

    @Json(name = "termClssFloorLmtActivate")
    val termClssFloorLmtActivate: Int = 1,

    @Json(name = "termClssLmtActivate")
    val termClssLmtActivate: Boolean = false,

    @Json(name = "programID")
    val programID: String? = null
) {
    fun toDrlV2(): DrlV2 {
        return DrlV2().apply {
            this@Drl.programID?.also { programID = ByteUtil.hexStr2Bytes(it) }
            isDefaultLmt = this@Drl.isDefaultLmt
            zeroCheck = this@Drl.zeroCheck.toByte()
            this@Drl.termClssLmt?.also { termClssLmt = ByteUtil.hexStr2Bytes(it) }
            this@Drl.termClssFloorLmt?.also { termClssFloorLmt = ByteUtil.hexStr2Bytes(it) }
            statusCheck = this@Drl.statusCheck
            this@Drl.cvmLmt?.also { cvmLmt = ByteUtil.hexStr2Bytes(it) }
            this@Drl.termFloorLmt?.also { termFloorLmt = ByteUtil.hexStr2Bytes(it) }
            cvmLmtActivate = this@Drl.cvmLmtActivate
            termClssFloorLmtActivate = this@Drl.termClssFloorLmtActivate.toByte()
            termClssLmtActivate = this@Drl.termClssLmtActivate
        }
    }
}
