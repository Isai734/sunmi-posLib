package com.pagatodo.sunmi.poslib.util

object Constants {
    const val track1 = "TRACK1"
    const val track2 = "TRACK2"
    const val track3 = "TRACK3"
    const val maskedPan = "maskedPan"
    const val encTrack1 = "encTrack1"
    const val encTrack2 = "encTrack2"
    const val encTrack3 = "encTrack3"
    const val cardholderName = "cardholderName"
    const val serviceCode = "serviceCode"
    const val pinBlock = "pinBlock"
    const val expiryDate = "expiryDate"

    //Operations
    const val DEVOLUCION = "D" //valor de prueba

    //Brands
    const val MASTERCARD = "A000000004101"
    const val MAESTRO = "A000000004306"

    //Pin types
    const val PIN_OFFLINE = 1
    const val PIN_ONLINE = 0

    //AidTypes
    const val contact = "01"
    const val contactLess = "02"
    const val forRefund = "20"
    const val normal = "00"

    enum class TransType(val type: String) {
        PURCHASE("00"), ADVANCE("01"), CASHBACK("09"), REFUND("20")
    }

    enum class EncrypType {
        TRACKENCRYPT, PANENCRYPT, ICCENCRYPT, PINENCRYPT
    }


    enum class TagsEmv(val tag: String) {
        ENC_PAN("5A"),
        ENC_TRACK_1("9F1F"),
        ENC_TRACK_2("57"),
        ENC_TRACK_3("58"),
        PIN_BLOCK("99"),
        TLV("tlv"),
        ICC_DATA("iccdata"),
        EXPIRE_DATE("5F24"),
        CARDHOLDER_NAME("5F20"),
        SERVICE_CODE("5F30");
    }

    enum class TlvResponses(val response: String, val status: Int) {
        Empty("", 0),
        Decline("8A023035", 2),
        Approved("8A023030", 0),
    }
}