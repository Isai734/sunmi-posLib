package com.pagatodo.sunmi.poslib.model

import com.pagatodo.sunmi.poslib.util.ByteUtil
import com.pagatodo.sunmi.poslib.util.Constants.normal
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.sunmi.pay.hardware.aidlv2.bean.AidV2

@JsonClass(generateAdapter = true)
data class Aid(
    @Json(name = "aidType")
    val aidType: String? = normal,

    @Json(name = "cvmCapability")
    val cvmCapability: String? = null,

    @Json(name = "merchCateCode")
    val merchCateCode: String? = null,

    @Json(name = "zeroCheck")
    val zeroCheck: String? = null,

    @Json(name = "maxTargetPer")
    val maxTargetPer: String? = null,

    @Json(name = "referCurrCode")
    val referCurrCode: String? = null,

    @Json(name = "threshold")
    val threshold: String? = null,

    @Json(name = "floorLimit")
    val floorLimit: String? = null,

    @Json(name = "kernelType")
    val kernelType: String? = null,

    @Json(name = "paramType")
    val paramType: String? = null,

    @Json(name = "termId")
    val termId: String? = null,

    @Json(name = "riskManData")
    val riskManData: String? = null,

    @Json(name = "referCurrCon")
    val referCurrCon: String? = null,

    @Json(name = "TACDenial")
    val tACDenial: String? = null,

    @Json(name = "dDOL")
    val dDOL: String? = null,

    @Json(name = "_comment")
    val comment: String? = null,

    @Json(name = "randTransSel")
    val randTransSel: String? = null,

    @Json(name = "TACOnline")
    val tACOnline: String? = null,

    @Json(name = "tDOL")
    val tDOL: String? = null,

    @Json(name = "termOfflineFloorLmt")
    val termOfflineFloorLmt: String? = null,

    @Json(name = "AcquierId")
    val acquierId: String? = null,

    @Json(name = "TACDefault")
    val tACDefault: String? = null,

    @Json(name = "merchName")
    val merchName: String? = null,

    @Json(name = "velocityCheck")
    val velocityCheck: String? = null,

    @Json(name = "referCurrExp")
    val referCurrExp: String? = null,

    @Json(name = "rMDLen")
    val rMDLen: String? = null,

    @Json(name = "termClssOfflineFloorLmt")
    val termClssOfflineFloorLmt: String? = null,

    @Json(name = "version")
    val version: String? = null,

    @Json(name = "merchId")
    val merchId: String? = null,

    @Json(name = "targetPer")
    val targetPer: String? = null,

    @Json(name = "ttq")
    val ttq: String? = null,

    @Json(name = "termClssLmt")
    val termClssLmt: String? = null,

    @Json(name = "cvmLmt")
    val cvmLmt: String? = null,

    @Json(name = "aid")
    val aid: String = "",

    @Json(name = "selFlag")
    val selFlag: String? = null,

    @Json(name = "clsStatusCheck")
    val clsStatusCheck: String? = null
) {

    fun toAidV2(): AidV2 {
        val aidV2 = AidV2()
        cvmLmt?.apply {
            aidV2.cvmLmt = ByteUtil.hexStr2Bytes(this)
        }
        termClssLmt?.apply {
            aidV2.termClssLmt = ByteUtil.hexStr2Bytes(this)
        }
        termClssOfflineFloorLmt?.apply {
            aidV2.termClssOfflineFloorLmt = ByteUtil.hexStr2Bytes(this)
        }
        termOfflineFloorLmt?.apply {
            aidV2.termOfflineFloorLmt = ByteUtil.hexStr2Bytes(this)
        }
        aid?.apply {
            aidV2.aid = ByteUtil.hexStr2Bytes(this)
        }
        selFlag?.apply {
            aidV2.selFlag = ByteUtil.hexStr2Byte(this)
        }
        targetPer?.apply {
            aidV2.targetPer = ByteUtil.hexStr2Byte(this)
        }
        maxTargetPer?.apply {
            aidV2.maxTargetPer = ByteUtil.hexStr2Byte(this)
        }
        floorLimit?.apply {
            aidV2.floorLimit = ByteUtil.hexStr2Bytes(this)
        }
        threshold?.apply {
            aidV2.threshold = ByteUtil.hexStr2Bytes(this)
        }
        tACDenial?.apply {
            aidV2.TACDenial = ByteUtil.hexStr2Bytes(this)
        }
        tACOnline?.apply {
            aidV2.TACOnline = ByteUtil.hexStr2Bytes(this)
        }
        tACDefault?.apply {
            aidV2.TACDefault = ByteUtil.hexStr2Bytes(this)
        }
        acquierId?.apply {
            aidV2.AcquierId = ByteUtil.hexStr2Bytes(this)
        }
        dDOL?.apply {
            aidV2.dDOL = ByteUtil.hexStr2Bytes(this)
        }
        version?.apply {
            aidV2.version = ByteUtil.hexStr2Bytes(this)
        }
        merchName?.apply {
            aidV2.merchName = ByteUtil.hexStr2Bytes(this)
        }
        merchCateCode?.apply {
            aidV2.merchCateCode = ByteUtil.hexStr2Bytes(this)
        }
        merchId?.apply {
            aidV2.merchId = ByteUtil.hexStr2Bytes(this)
        }
        referCurrCode?.apply {
            aidV2.referCurrCode = ByteUtil.hexStr2Bytes(this)
        }
        referCurrExp?.apply {
            aidV2.referCurrExp = ByteUtil.hexStr2Byte(this)
        }
        clsStatusCheck?.apply {
            aidV2.clsStatusCheck = ByteUtil.hexStr2Byte(this)
        }
        zeroCheck?.apply {
            aidV2.zeroCheck = ByteUtil.hexStr2Byte(this)
        }
        kernelType?.apply {
            aidV2.kernelType = ByteUtil.hexStr2Byte(this)
        }
        paramType?.apply {
            aidV2.paramType = ByteUtil.hexStr2Byte(this)
        }
        ttq?.apply {
            aidV2.ttq = ByteUtil.hexStr2Bytes(this)
        }
        termId?.apply {
            aidV2.termId = ByteUtil.hexStr2Bytes(this)
        }
        riskManData?.apply {
            aidV2.riskManData = ByteUtil.hexStr2Bytes(this)
        }
        referCurrCon?.apply {
            aidV2.referCurrCon = ByteUtil.hexStr2Bytes(this)
        }
        tDOL?.apply {
            aidV2.tDOL = ByteUtil.hexStr2Bytes(this)
        }
        return aidV2
    }

    override fun toString(): String {
        return "Aid(aidType=$aidType, cvmCapability=$cvmCapability, floorLimit=$floorLimit, riskManData=$riskManData, tACDenial=$tACDenial, dDOL=$dDOL, comment=$comment, tACOnline=$tACOnline, tDOL=$tDOL, termOfflineFloorLmt=$termOfflineFloorLmt, acquierId=$acquierId, tACDefault=$tACDefault, termClssOfflineFloorLmt=$termClssOfflineFloorLmt, version=$version, ttq=$ttq, termClssLmt=$termClssLmt, cvmLmt=$cvmLmt, aid=$aid)"
    }


}
