package com.pagatodo.sunmi.poslib.config

import android.util.Log
import com.pagatodo.sunmi.poslib.model.Aid
import com.pagatodo.sunmi.poslib.model.Capk
import com.pagatodo.sunmi.poslib.model.Drl
import com.pagatodo.sunmi.poslib.util.ByteUtil
import com.pagatodo.sunmi.poslib.util.Constants.DEVOLUCION
import com.pagatodo.sunmi.poslib.util.Constants.MAESTRO
import com.pagatodo.sunmi.poslib.util.Constants.MASTERCARD
import com.pagatodo.sunmi.poslib.util.Constants.contactLess
import com.pagatodo.sunmi.poslib.util.Constants.forRefund
import java.util.*

class PosConfig {

    var security = EncryptKeys(
        plainDataKey = ByteUtil.hexStr2Bytes("F40379AB9E0EC533F40379AB9E0EC533"),
        plainDataKcvKey = ByteUtil.hexStr2Bytes("82E13665B4624DF5"),
        plainPinKey = ByteUtil.hexStr2Bytes("F40379AB9E0EC533F40379AB9E0EC533"),
        plainPinKcvKey = ByteUtil.hexStr2Bytes("82E13665B4624DF5")
    )

    var capks: List<Capk> = LinkedList<Capk>()
    var aids: List<Aid> = LinkedList<Aid>()
    var drls: List<Drl> = LinkedList<Drl>()
    internal val paypassConfig get() = PaypassConfig(aids)

    class EncryptKeys(
        val plainDataKey: ByteArray,
        val plainDataKcvKey: ByteArray,
        val plainPinKey: ByteArray,
        val plainPinKcvKey: ByteArray,
        val keyDataIndex: Int = 10,
        val keyPinIndex: Int = 11
    )

    class PaypassConfig(val aids: List<Aid> = LinkedList()) {

        fun getConfig(config: String): Aid {
            return when (config) {
                MASTERCARD,
                MAESTRO -> findByAid(config)
                DEVOLUCION -> finByOperation(forRefund)
                else -> Aid()
            }.apply { Log.d("PosConfig", toString()) }
        }

        private fun findByAid(brand: String): Aid {
            var fAid = Aid()
            for (aid in aids) {
                aid.aid.apply {
                    if (startsWith(brand) && aid.aidType == contactLess)
                        fAid = aid
                }
            }
            return fAid
        }

        private fun finByOperation(operation: String): Aid {
            var fAid = Aid()
            for (aid in aids) {
                aid.aidType?.apply {
                    if (this == operation)
                        fAid = aid
                }
            }
            return fAid
        }

    }
}