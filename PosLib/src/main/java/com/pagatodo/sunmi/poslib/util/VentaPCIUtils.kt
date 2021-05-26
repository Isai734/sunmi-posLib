package com.pagatodo.sunmi.poslib.util

object VentaPCIUtils {

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