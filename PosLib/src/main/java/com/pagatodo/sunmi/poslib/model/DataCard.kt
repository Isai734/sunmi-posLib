package com.pagatodo.sunmi.poslib.model

import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.Constants
import com.sunmi.pay.hardware.aidl.bean.CardInfo
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.util.HexUtil
import java.nio.charset.Charset

class DataCard(var tlvData: String = "", var holderName: String = "", var pinBlock: String? = null, var entryMode: DataOpTarjeta.PosEntryMode? = null, var mapTags: Map<String, String> = HashMap(), var monthlyPayments: Int = 0, var daysDeferred: Int = 0, var cvv: Int? = null) : CardInfo() {

    val panEncrypt: ByteArray?
        get() {
            return posInstance().encryptUtil.onEncryptData(
                stringToByteArray(cardNo, entryMode!!),
                Constants.EncrypType.PANENCRYPT
            )
        }

    val track1Encrypt: ByteArray?
        get() {
            return posInstance().encryptUtil.onEncryptData(
                stringToByteArray(track1, entryMode!!),
                Constants.EncrypType.TRACKENCRYPT
            )
        }

    val track2Encrypt: ByteArray?
        get() {
            return posInstance().encryptUtil.onEncryptData(
                stringToByteArray(track2, entryMode!!),
                Constants.EncrypType.TRACKENCRYPT
            )
        }

    val track3Encrypt: ByteArray?
        get() {
            return posInstance().encryptUtil.onEncryptData(
                stringToByteArray(track3, entryMode!!),
                Constants.EncrypType.TRACKENCRYPT
            )
        }
    val icDataEncrypt: ByteArray?
        get() {
            return posInstance().encryptUtil.onEncryptData(
                stringToByteArray(tlvData, entryMode!!),
                Constants.EncrypType.ICCENCRYPT
            )
        }

    val pinEncrypt: ByteArray?
        get() {
            return pinBlock?.let { HexUtil.hex2byte(pinBlock, Charset.defaultCharset()) }
        }

    private fun stringToByteArray(string: String, entryMode: DataOpTarjeta.PosEntryMode): ByteArray {
        return if (entryMode == DataOpTarjeta.PosEntryMode.BANDA || entryMode == DataOpTarjeta.PosEntryMode.FALLBACK)
            string.toByteArray(Charsets.ISO_8859_1)
        else
            HexUtil.hex2byte(string, Charset.defaultCharset())
    }

    override fun toString(): String {
        return "DataCard(tlvData='$tlvData', holderName='$holderName', pinBlock='$pinBlock', entryMode=$entryMode, monthlyPayments=$monthlyPayments, daysDeferred=$daysDeferred, ${super.toString()})"
    }
}