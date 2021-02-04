package com.pagatodo.sunmi.poslib.util

import android.os.Bundle
import android.util.Log
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.SunmiTransaction
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.Constants.normal
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2
import java.util.regex.Pattern

object EmvUtil {
    private val TAG = EmvUtil::class.java.simpleName
    val tagsDefault = arrayOf<String>(
        "DF02",
        "5F34",
        "5F36",
        "9F06",
        "FF30",
        "FF31",
        "95",
        "9B",
        "9F36",
        "9F26",
        "9F27",
        "DF31",
        "5A",
        "57",
        "5F24",
        "9F1A",
        "9F33",
        "9F35",
        "9F40",
        "9F03",
        "9F10",
        "9F37",
        "9C",
        "9A",
        "9F02",
        "9F0D",
        "5F2A",
        "82",
        "9F34",
        "9F1E",
        "84",
        "8E",
        "47",
        "4F",
        "9F66",
        "9F6C",
        "9F09",
        "9F41",
        "9F63",
        "5F20",
        "5F30",
        "9F12",
        "50",
        "DF13"
    )
    val payPassTags = arrayOf(
        "DF811E",
        "DF812C",
        "DF8118",
        "DF8119",
        "DF811F",
        "DF8117",
        "DF8124",
        "DF8125",
        "DF8126",
        "9F6D",
        "9F6E",
        "DF811B",
        "9F53",
        "DF810C",
        "9F1D",
        "DF8130",
        "DF812D",
        "DF811C",
        "DF811D",
        "9F7C"
    )

    fun setTerminalParam(params: EmvTermParamV2?, emvOptV2: EMVOptV2) {
        try {
            val result = emvOptV2.setTerminalParam(params)
            Log.i(TAG, "setTerminalParam result: $result -> params ${params.toString()}")
        } catch (e: Exception) {
            Log.i(TAG, "setTerminalParam fail $e")
        }
    }

    fun initKey(mSecurityOptV2: SecurityOptV2) { //Initialize keys
        try {
            // save TMK
            val security = posInstance().posConfig.security
            var result = mSecurityOptV2.savePlaintextKey(AidlConstants.Security.KEY_TYPE_TDK, security.plainDataKey, security.plainDataKcvKey, AidlConstants.Security.KEY_ALG_TYPE_3DES, 10)
            if (result != 0) {
                PosLogger.e(TAG, "save TDK fail: $result")
                return
            }
            result = mSecurityOptV2.savePlaintextKey(AidlConstants.Security.KEY_TYPE_PIK, security.plainPinkey, security.plainPinKcvkey, AidlConstants.Security.KEY_ALG_TYPE_3DES, 11)
            if (result != 0) {
                PosLogger.e(TAG, "save PIK fail: $result")
                return
            }
            PosLogger.d(TAG, "init  key success")
        } catch (e: Exception) {
            PosLogger.e(TAG, "init key fail $e")
        }
    }

    fun setCapks(emvOptV2: EMVOptV2) {//Load public CA for aids
        emvOptV2.deleteCapk(null, null)
        for (capk in posInstance().posConfig.capks) {
            emvOptV2.addCapk(capk.toCapkV2())
        }
    }

    fun setAids(emvOptV2: EMVOptV2) {//Load application aids
        emvOptV2.deleteAid(null)
        for (aid in posInstance().posConfig.aids) {
            if (aid.aidType == normal)
                emvOptV2.addAid(aid.toAidV2())
        }
    }

    fun setDlr(emvOptV2: EMVOptV2) {//Load application aids
        val bundle = Bundle().apply {
            this.putBoolean("supportDRL", true)
        }
        emvOptV2.deleteDrlLimitSet(null)
        emvOptV2.setTermParamEx(bundle)
        for (drl in posInstance().posConfig.drls) {
            emvOptV2.addDrlLimitSet(drl.toDrlV2())
        }
    }

    fun isChipCard(serviceCode: String): Boolean {
        return if (serviceCode.isNotEmpty()) {
            serviceCode[0] == '2' || serviceCode[0] == '6'
        } else false
    }

    fun requiredNip(serviceCode: String): Boolean {
        return serviceCode[2] == '0' || serviceCode[2] == '3' || serviceCode[2] == '5' || serviceCode[2] == '6' || serviceCode[2] == '7'
    }

    fun parseTrack2(track2: String?): DataCard {//Parse track2 data
        val mTrack2 = stringFilter(track2)
        var index = mTrack2.indexOf("=")
        if (index == -1) {
            index = mTrack2.indexOf("D")
        }
        val cardInfo = DataCard()
        if (index == -1) {
            return cardInfo
        }
        var cardNumber = ""
        if (mTrack2.length > index) {
            cardNumber = mTrack2.substring(0, index)
        }
        var expiryDate = ""
        if (mTrack2.length > index + 5) {
            expiryDate = mTrack2.substring(index + 1, index + 5)
        }
        var serviceCode = ""
        if (mTrack2.length > index + 8) {
            serviceCode = mTrack2.substring(index + 5, index + 8)
        }
        PosLogger.i(
            PosLib.TAG,
            "cardNumber: $cardNumber expireDate: $expiryDate serviceCode: $serviceCode"
        )
        cardInfo.cardNo = cardNumber
        cardInfo.expireDate = expiryDate
        cardInfo.serviceCode = serviceCode
        return cardInfo
    }

    private fun stringFilter(str: CharSequence?): String {//remove characters not number,=,D
        val regEx = "[^0-9=D]"
        val p = Pattern.compile(regEx)
        val matcher = p.matcher(str ?: "")
        return matcher.replaceAll("").trim { it <= ' ' }
    }
}