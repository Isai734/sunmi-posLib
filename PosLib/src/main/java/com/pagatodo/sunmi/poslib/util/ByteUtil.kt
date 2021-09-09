package com.pagatodo.sunmi.poslib.util

import java.util.*

object ByteUtil {
    fun bytes2HexStr(bytes: ByteArray): String {
        val sb = StringBuilder()
        var temp: String
        for (b in bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            temp = Integer.toHexString(0xFF and b.toInt())
            if (temp.length == 1) {
                // 每个字节8为，转为16进制标志，2个16进制位
                sb.append("0")
            }
            sb.append(temp)
        }
        return sb.toString().uppercase(Locale.ROOT)
    }

    fun hexStr2Bytes(hexStr: String): ByteArray {
        val hexStr1 = hexStr.lowercase(Locale.ROOT)
        val length = hexStr1.length
        val bytes = ByteArray(length shr 1)
        var index = 0
        for (i in 0 until length) {
            if (index > hexStr1.length - 1) return bytes
            val highDit = (Character.digit(hexStr1[index], 16) and 0xFF)
            val lowDit = (Character.digit(hexStr1[index + 1], 16) and 0xFF)
            bytes[i] = (highDit shl 4 or lowDit).toByte()
            index += 2
        }
        return bytes
    }

    fun hexStr2Byte(hexStr: String): Byte {
        return hexStr.toInt(16).toByte()
    }
}