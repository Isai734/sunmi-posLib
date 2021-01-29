package com.pagatodo.sunmi.poslib.util

import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.util.HexUtil
import java.nio.charset.Charset
import java.util.*

object VentaPCIUtils {

    fun orderTags(array: List<String>): List<String> {
        val nList = LinkedList<String>()
        val tagList = arrayOf(
                "DF02", "5F34", "5F36", "9F06", "FF30", "FF31", "95", "9B", "9F36", "9F26",
                "9F27", "DF31", "5A", "57", "5F24", "9F1A", "9F33", "9F35", "9F40",
                "9F03", "9F10", "9F37", "9C", "9A", "9F02", "9F0D", "5F2A", "82", "9F34", "9F1E",
                "84", "8E", "47", "4F", "9F66", "9F6C", "9F09", "9F41", "9F63", "5F20", "5F30", "9F12", "50", "DF13"
        )
        for (tagp in tagList) {
            for (tagl in array) {
                if (tagp.equals(tagl, true))
                    nList.add(tagp)
            }
        }
        return tagList.toList()//
    }

    fun getByteArrayFromEntryMode(string: String, entryMode: DataOpTarjeta.PosEntryMode): ByteArray {
        return if (entryMode == DataOpTarjeta.PosEntryMode.BANDA) string.toByteArray(Charsets.ISO_8859_1) else HexUtil.hex2byte(string, Charset.defaultCharset())
    }

    fun emvRequestSignature(tags: String): Boolean {
        val cvmResult = finTag(tags, "9F34")
        return if (cvmResult.isNotEmpty() && cvmResult.length == 6) {
            val firstByte = cvmResult.substring(0, 2)
            val hexRule = firstByte.toInt(16)
            (hexRule == 0x03 // Plaintext PIN by ICC & Signature
                    || hexRule == 0x05 // Enciphered PIN by ICC & Signature
                    || hexRule == 0x1E) // Signature
        } else {
            false
        }
    }

    fun finTag(tags: String, tagFind: String): String {
        val index = tags.indexOf(tagFind, ignoreCase = true)
        return if (index != -1) {
            val tag = tags.substring(index)
            val length = tags.substring(index + tagFind.length, index + tagFind.length + 2).toInt(16)
            tags.substring(tag.length + 2, tag.length + 2 + (length * 2))
        } else
            return ""
    }
}