package com.pagatodo.sunmi.poslib.keychain

import android.os.RemoteException
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.Constants
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2
import java.util.*

class CryptUtil(private val mSecurityOptV2: SecurityOptV2) {
    @Throws(RemoteException::class)
    private fun getTrackEncrypt(selectTAG: ByteArray, keyIndex: Int): ByteArray {
        val dataIn = createBytePaddingTrack(selectTAG)
        val dataOut = ByteArray(dataIn.size)
        val result = mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstants.Security.DATA_MODE_ECB, null, dataOut)
        return if (result == 0) {
            dataOut
        } else {
            ByteArray(0)
        }
    }

    @Throws(RemoteException::class)
    private fun getByteEncrypt(dataIn: ByteArray, keyIndex: Int): ByteArray {
        val dataOut = ByteArray(dataIn.size)
        val result = mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstants.Security.DATA_MODE_ECB, null, dataOut)
        return if (result == 0) {
            dataOut
        } else {
            ByteArray(0)
        }
    }

    private fun onPosTrackEncrypt(bytes: ByteArray): ByteArray? {
        return try {
            getTrackEncrypt(bytes, posInstance().posConfig.security.keyDataIndex)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun onPosEncryptData(bytes: ByteArray): ByteArray? {
        return try {
            getByteEncrypt(createBytePadding(bytes), posInstance().posConfig.security.keyDataIndex)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun onPosPINEncrypt(bytes: ByteArray): ByteArray? {
        return try {
            getByteEncrypt(bytes, posInstance().posConfig.security.keyPinIndex)
        } catch (e: RemoteException) {
            null
        }
    }

    fun onEncryptData(bytes: ByteArray, type: Constants.EncrypType): ByteArray? {
        return when (type) {
            Constants.EncrypType.PINENCRYPT -> onPosPINEncrypt(bytes)
            Constants.EncrypType.TRACKENCRYPT -> onPosTrackEncrypt(bytes)
            Constants.EncrypType.PANENCRYPT, Constants.EncrypType.ICCENCRYPT -> onPosEncryptData(bytes)
        }
    }

    private fun createBytePadding(input: ByteArray): ByteArray {
        var len = input.size
        return if (input.size % 8 > 0) {
            len += 8 - input.size % 8
            val ret = ByteArray(len)
            Arrays.fill(ret, 0x00.toByte())
            System.arraycopy(input, 0, ret, 0, input.size)
            ret
        } else {
            input
        }
    }

    private fun createBytePaddingTrack(input: ByteArray): ByteArray {
        var len = input.size
        return if (input.size % 8 > 0) {
            len += 8 - input.size % 8
            val ret = ByteArray(len)
            Arrays.fill(ret, 0xFF.toByte())
            System.arraycopy(input, 0, ret, 0, input.size)
            ret
        } else {
            input
        }
    }
}