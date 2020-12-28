package com.pagatodo.sunmi.poslib.model

import com.sunmi.pay.hardware.aidl.bean.CardInfo

class DataCard(
    var tlvData: String = "",
    var holderName: String = "",
    var pinBlock: String = "",
) : CardInfo() {
    override fun toString(): String {
        return "DataCard(tlvData='$tlvData', holderName='$holderName', pinBlock='$pinBlock') ${super.toString()}"
    }
}