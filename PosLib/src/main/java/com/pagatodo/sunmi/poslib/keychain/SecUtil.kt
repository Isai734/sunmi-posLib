package com.pagatodo.sunmi.poslib.keychain

import android.os.RemoteException
import com.pagatodo.sunmi.poslib.util.Constants
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2
import java.util.*

class SecUtil(private val mSecurityOptV2: SecurityOptV2) {
    @Throws(RemoteException::class)
     fun getTrackEncrypt(selectTAG: ByteArray, keyIndex: Int): ByteArray {
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
     fun getPanEncrypt(selectTAG: ByteArray, keyIndex: Int): ByteArray {
        val dataIn = createBytePadding(selectTAG)
        val dataOut = ByteArray(dataIn.size)
        val result = mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstants.Security.DATA_MODE_ECB, null, dataOut)
        return if (result == 0) {
            dataOut
        } else {
            ByteArray(0)
        }
    }

    @Throws(RemoteException::class)
     fun getByteEncrypt(data: ByteArray?, keyIndex: Int): ByteArray {
        val dataOut = ByteArray(data!!.size)
        val result = mSecurityOptV2.dataEncrypt(keyIndex, data, AidlConstants.Security.DATA_MODE_ECB, null, dataOut)
        if (result == 0) {
            return dataOut
        }
        return ByteArray(0)
    }

    private fun onPosPanEncrypt(bytes: ByteArray): ByteArray? {
        return try {
            getPanEncrypt(bytes, 10)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun onPosTrackEncrypt(bytes: ByteArray): ByteArray? {
        return try {
            getTrackEncrypt(bytes, 10)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }


    private fun onPosPINEncrypt(bytes: ByteArray?): ByteArray? {
        return try {
            getByteEncrypt(bytes, 11)
        } catch (e: RemoteException) {
            null
        }
    }

    private fun onPosEncryptData(bytes: ByteArray): ByteArray? {
        return try {
            getByteEncrypt(createBytePadding(bytes), 10)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    fun onEncryptData(bytes: ByteArray, type: Constants.EncrypType): ByteArray? {
        return when (type) {
            Constants.EncrypType.PANENCRYPT -> onPosPanEncrypt(bytes)
            Constants.EncrypType.TRACKENCRYPT -> onPosTrackEncrypt(bytes)
            Constants.EncrypType.ICCENCRYPT -> onPosEncryptData(bytes)
            Constants.EncrypType.PINENCRYPT -> onPosPINEncrypt(bytes)
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