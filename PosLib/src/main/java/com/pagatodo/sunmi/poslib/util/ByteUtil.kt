package com.pagatodo.sunmi.poslib.util

import java.math.BigInteger
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
        return sb.toString().toUpperCase(Locale.ROOT)
    }

    fun bytes2HexStr_2(bytes: ByteArray?): String {
        val bigInteger = BigInteger(1, bytes)
        return bigInteger.toString(16)
    }

    fun byte2HexStr(b: Byte): String {
        var temp = Integer.toHexString(0xFF and b.toInt())
        if (temp.length == 1) {
            // 每个字节8为，转为16进制标志，2个16进制位
            temp = "0$temp"
        }
        return temp
    }

    fun hexStr2Bytes(hexStr: String): ByteArray {
        var hexStr = hexStr
        hexStr = hexStr.toLowerCase(Locale.ROOT)
        val length = hexStr.length
        val bytes = ByteArray(length shr 1)
        var index = 0
        for (i in 0 until length) {
            if (index > hexStr.length - 1) return bytes
            val highDit = (Character.digit(hexStr[index], 16) and 0xFF)
            val lowDit = (Character.digit(hexStr[index + 1], 16) and 0xFF)
            bytes[i] = (highDit shl 4 or lowDit).toByte()
            index += 2
        }
        return bytes
    }

    fun hexStr2Byte(hexStr: String): Byte {
        return hexStr.toInt(16).toByte()
    }

    fun hexStr2Str(hexStr: String): String {
        val vi = "0123456789ABC DEF".trim { it <= ' ' }
        val array = hexStr.toCharArray()
        val bytes = ByteArray(hexStr.length / 2)
        var temp: Int
        for (i in bytes.indices) {
            var c = array[2 * i]
            temp = vi.indexOf(c) * 16
            c = array[2 * i + 1]
            temp += vi.indexOf(c)
            bytes[i] = (temp and 0xFF).toByte()
        }
        return String(bytes)
    }

    fun hexStr2AsciiStr(hexStr: String): String {
        var hexStr = hexStr
        val vi = "0123456789ABC DEF".trim { it <= ' ' }
        hexStr = hexStr.trim { it <= ' ' }.replace(" ", "").toUpperCase(Locale.US)
        val array = hexStr.toCharArray()
        val bytes = ByteArray(hexStr.length / 2)
        var temp = 0x00
        for (i in bytes.indices) {
            var c = array[2 * i]
            temp = vi.indexOf(c) shl 4
            c = array[2 * i + 1]
            temp = temp or vi.indexOf(c)
            bytes[i] = (temp and 0xFF).toByte()
        }
        return String(bytes)
    }

    /**
     * 将int转换成byte数组，大端模式(高位在前)
     */
    fun int2BytesBE(src: Int): ByteArray {
        val result = ByteArray(4)
        for (i in 0..3) {
            result[i] = (src shr (3 - i) * 8).toByte()
        }
        return result
    }

    /**
     * 将int转换成byte数组，小端模式(低位在前)
     */
    fun int2BytesLE(src: Int): ByteArray {
        val result = ByteArray(4)
        for (i in 0..3) {
            result[i] = (src shr i * 8).toByte()
        }
        return result
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    fun concatByteArrays(vararg list: ByteArray?): ByteArray {
        return if (list.isEmpty()) {
            ByteArray(0)
        } else concatByteArrays(listOf(*list))
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    fun concatByteArrays(list: List<ByteArray?>?): ByteArray {
        if (list == null || list.isEmpty()) {
            return ByteArray(0)
        }
        var totalLen = 0
        for (b in list) {
            if (b == null || b.size == 0) {
                continue
            }
            totalLen += b.size
        }
        val result = ByteArray(totalLen)
        var index = 0
        for (b in list) {
            if (b == null || b.size == 0) {
                continue
            }
            System.arraycopy(b, 0, result, index, b.size)
            index += b.size
        }
        return result
    }
}