package com.pagatodo.sunmi.poslib.model

import com.pagatodo.sunmi.poslib.util.Constants
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2
import java.util.*

class TransactionData {
    var amount = "000"
    var otherAmount: String = "0"
    var currencyCode: String? = null
    var sigmaOperation: String? = null
    var decimals: Int? = null
    var totalAmount: String = "0"
    var cashBackLector: String = "0"
    var cashBackAmount: String = "0"
    var gratuity: String = "0"
    var taxes: String = "0"
    var comisions: String = "0"
    var zipCode: String? = null
    var tagsEmv: List<String> = LinkedList()
    var transType = Constants.TransType.PURCHASE
    var terminalParams: EmvTermParamV2 = EmvTermParamV2()

    override fun toString(): String {
        return "TransactionData(amount= '$amount', cashbackAmount= '$otherAmount', currencyCode= $currencyCode, tipoOperacion= $sigmaOperation, decimales= $decimals, importeOperacion= '$totalAmount', importeCashback= '$cashBackAmount', importePropina= '$gratuity', importeImpuestos= '$taxes', comision= '$comisions', codigoPostal= $zipCode, tagsEmv= $tagsEmv, transType= $transType, terminalParams= $terminalParams)"
    }

}