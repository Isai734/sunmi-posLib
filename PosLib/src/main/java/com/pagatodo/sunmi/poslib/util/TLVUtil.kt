package com.pagatodo.sunmi.poslib.util

import android.text.TextUtils
import com.pagatodo.sunmi.poslib.model.TLV
import java.util.*

class TLVUtil private constructor() {
    companion object {
        /**
         * 将16进制字符串转换为TLV对象列表
         *
         * @param hexStr Hex格式的TLV数据
         * @return TLV数据List
         */
        fun buildTLVList(hexStr: String): List<TLV> {
            val list: MutableList<TLV> = ArrayList()
            var position = 0
            while (position != hexStr.length) {
                val tupleTag: Pair<String, Int> = getTag(hexStr, position)
                if (TextUtils.isEmpty(tupleTag.first) || "00" == tupleTag.first) {
                    break
                }
                val tupleLen: Pair<Int, Int> = getLength(hexStr, tupleTag.second)
                val tupleValue: Pair<String, Int> = getValue(hexStr, tupleLen.second, tupleLen.first)
                //            Log.e("TLV-buildTLVList", tupleTag.a + ":" + tupleValue.a);
                list.add(TLV(tupleTag.first, tupleLen.first, tupleValue.first))
                position = tupleValue.second
            }
            return list
        }

        /**
         * 将16进制字符串转换为TLV对象MAP<br></br>
         * TLV文档连接参照：http://wenku.baidu.com/view/b31b26a13186bceb18e8bb53.html?re=view&qq-pf-to=pcqq.c2c
         *
         * @param hexStr Hex格式TLV数据
         * @return TLV数据Map
         */
        fun buildTLVMap(hexStr: String): Map<String, TLV> {
            val map: MutableMap<String, TLV> = LinkedHashMap()
            if (TextUtils.isEmpty(hexStr) || hexStr.length % 2 != 0) return map
            var position = 0
            while (position < hexStr.length) {
                val tupleTag: Pair<String, Int> = getTag(hexStr, position)
                if (TextUtils.isEmpty(tupleTag.first) || "00" == tupleTag.first) {
                    break
                }
                val tupleLen: Pair<Int, Int> = getLength(hexStr, tupleTag.second)
                val tupleValue: Pair<String, Int> = getValue(hexStr, tupleLen.second, tupleLen.first)
                //            Log.e("TLV-buildTLVMap", tupleTag.a + ":" + tupleValue.a);
                map[tupleTag.first] = TLV(tupleTag.first, tupleLen.first, tupleValue.first)
                position = tupleValue.second
            }
            return map
        }

        /**
         * 将字节数组转换为TLV对象MAP
         *
         * @param hexByte byte数据格式的TLV数据
         * @return TLV数据Map
         */
        fun buildTLVMap(hexByte: ByteArray): Map<String, TLV> {
            val hexString: String = ByteUtil.bytes2HexStr(hexByte)
            return buildTLVMap(hexString)
        }

        /**
         * 获取Tag及更新后的游标位置
         */
        private fun getTag(hexString: String, position: Int): Pair<String, Int> {
            var tag = ""
            try {
                val byte1 = hexString.substring(position, position + 2)
                val byte2 = hexString.substring(position + 2, position + 4)
                val b1 = byte1.toInt(16)
                val b2 = byte2.toInt(16)
                // b5~b1如果全为1，则说明这个tag下面还有一个子字节，PBOC/EMV里的tag最多占两个字节
                tag = if (b1 and 0x1F == 0x1F) {
                    // 除tag标签首字节外，tag中其他字节最高位为：1-表示后续还有字节；0-表示为最后一个字节。
                    if (b2 and 0x80 == 0x80) {
                        hexString.substring(position, position + 6) // 3Bytes的tag
                    } else {
                        hexString.substring(position, position + 4) // 2Bytes的tag
                    }
                } else {
                    hexString.substring(position, position + 2) // 1Bytes的tag
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Pair(tag.toUpperCase(Locale.ROOT), position + tag.length)
        }

        /**
         * 获取Length及游标更新后的游标位置
         */
        private fun getLength(hexStr: String, position: Int): Pair<Int, Int> {
            var index = position
            var hexLen = hexStr.substring(index, index + 2)
            index += 2
            val byte1 = hexLen.toInt(16)
            // Length域的编码比较简单,最多有四个字节, 
            // 如果第一个字节的最高位b8为0, b7~b1的值就是value域的长度. 
            // 如果b8为1, b7~b1的值指示了下面有几个子字节. 下面子字节的值就是value域的长度.
            if (byte1 and 0x80 != 0) { // 最左侧的bit位为1
                val subLen = byte1 and 0x7F
                hexLen = hexStr.substring(index, index + subLen * 2)
                index += subLen * 2
            }
            return Pair(hexLen.toInt(16), index)
        }

        /**
         * 获取Value及游标更新后的游标位置
         */
        private fun getValue(hexStr: String, position: Int, len: Int): Pair<String, Int> {
            var value = ""
            try {
                value = hexStr.substring(position, position + len * 2)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Pair(value.toUpperCase(Locale.ROOT), position + len * 2)
        }

        /***
         * 将TLV转换成16进制字符串
         */
        fun revertToHexStr(tlv: TLV): String {
            val sb = StringBuilder()
            sb.append(tlv.tag)
            sb.append(TLVValueLengthToHexString(tlv.length))
            sb.append(tlv.value)
            return sb.toString()
        }

        /**
         * 将TLV数据反转成字节数组
         */
        fun revertToBytes(tlv: TLV): ByteArray {
            val hex = revertToHexStr(tlv)
            return ByteUtil.hexStr2Bytes(hex)
        }

        /**
         * 将TLV中数据长度转化成16进制字符串
         */
        fun TLVValueLengthToHexString(length: Int): String {
            if (length < 0) {
                throw RuntimeException("不符要求的长度")
            }
            return if (length <= 0x7f) {
                String.format("%02x", length)
            } else if (length <= 0xff) {
                "81" + String.format("%02x", length)
            } else if (length <= 0xffff) {
                "82" + String.format("%04x", length)
            } else if (length <= 0xffffff) {
                "83" + String.format("%06x", length)
            } else {
                throw RuntimeException("TLV 长度最多4个字节")
            }
        }
    }

    init {
        throw AssertionError("Create instance of TLVUtil is prohibited")
    }
}