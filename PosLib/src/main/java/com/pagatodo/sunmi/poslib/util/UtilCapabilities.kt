package com.pagatodo.sunmi.poslib.util

import com.pagatodo.sigmalib.emv.PerfilEmvApp
import com.pagatodo.sunmi.poslib.util.PosLogger.e
import java.util.*

object UtilCapabilities {
    private val TAG = UtilCapabilities::class.java.simpleName
    fun additionalTerminalCapabilitiesCode(): String {
        var transactionTypeResult = 0x0000
        var terminalDataInputResult = 0x00
        var dataOutputResult = 0x0000

        // Transaction Type Sum
        transactionTypeResult += TRANSACTION_TYPE.CASH.code
        transactionTypeResult += TRANSACTION_TYPE.GOODS.code
        transactionTypeResult += TRANSACTION_TYPE.SERVICES.code
        transactionTypeResult += TRANSACTION_TYPE.CASHBACK.code

        // Terminal Data Input Sum
        terminalDataInputResult += TERMINAL_DATA_INPUT.NUMERIC_KEYS.code
        terminalDataInputResult += TERMINAL_DATA_INPUT.ALPHA_AND_SPECIAL_CHARACTERS.code
        terminalDataInputResult += TERMINAL_DATA_INPUT.COMMAND_KEYS.code
        terminalDataInputResult += TERMINAL_DATA_INPUT.FUNCTION_KEYS.code

        // Data Output Sum
        dataOutputResult += DATA_OUTPUT.PRINT_ATTENDANT.code
        dataOutputResult += DATA_OUTPUT.DISPLAY_ATTENDANT.code
        dataOutputResult += DATA_OUTPUT.CODE_TABLE_1.code
        return Integer.toHexString(transactionTypeResult) +
                Integer.toHexString(terminalDataInputResult) +
                Integer.toHexString(dataOutputResult)
    }

    /**
     * See https://www.eftlab.com/the-use-of-ctqs-and-ttqs-in-nfc-transactions
     *
     * @param perfilesEmv
     * @return Value of TTQ
     */
    fun createTTQ(perfilesEmv: PerfilEmvApp): String {
        var firstByte = 0x30
        var secondByte = 0x00
        val thirdByte = 0x40
        if (perfilesEmv.perfilesEmv != null) {
            if (perfilesEmv.perfilesEmv.cvmPinOnline == 1) {
                firstByte = firstByte or 0x04
            }
            if (perfilesEmv.perfilesEmv.cvmFirma == 1) {
                firstByte = firstByte or 0x02
            }
            if (perfilesEmv.perfilesEmv.cvmPinOffline == 1) {
                secondByte = secondByte or 0x20
            }
        }
        return String.format(Locale.US, "%02x%02x%02x80", firstByte, secondByte, thirdByte)
    }

    fun terminalCapabilitiesCode(perfilesEmv: PerfilEmvApp): String {
        var codigoCapabilities = "60"
        if (perfilesEmv.perfilesEmv != null) {
            var result = 0x00
            try {
                /*VALIDAMOS EL TIPO DE CONFIGURACION QUE TENDRA EL DONGGLE*/
                if (perfilesEmv.perfilesEmv.cvmPinOnline == 1) {
                    result += CVM_TYPE.PINONLINE.cvmtype()
                }
                if (perfilesEmv.perfilesEmv.cvmPinOffline == 1) {
                    result += CVM_TYPE.PINOFFLINE.cvmtype() + CVM_TYPE.PLAINTEXTPINICC.cvmtype()
                }
                if (perfilesEmv.perfilesEmv.cvmFirma == 1) {
                    result += CVM_TYPE.SIGNATURE.cvmtype()
                }
                if (perfilesEmv.perfilesEmv.cvmNocvm == 1){
                    result += CVM_TYPE.NOCVM.cvmtype()
                }
                /*AGREGAMOS EL CODIGO DE LA VALIDACION DE PIN */
                val stringBufferCodigoCapabilities = StringBuilder()
                var strCmv = Integer.toHexString(result)
                if (strCmv.length == 1) strCmv = "0$strCmv"
                stringBufferCodigoCapabilities.append(codigoCapabilities).append(strCmv).append(
                    securityMethods
                )
                codigoCapabilities = stringBufferCodigoCapabilities.toString()
            } catch (ex: Exception) {
                PosLogger.e(TAG, "Error al Configurar las Capacidades del dongle")
            }
        } else {
            return "6008C8"
        }
        return codigoCapabilities
    }

    val securityMethods: String
        get() {
            var result = 0x00
            try {
                for (st in SECURITY_TYPE.values()) {
                    result += st.securityType()
                }
            } catch (ex: Exception) {
                PosLogger.e(TAG, "Error al Obtener Metodos de Segurridad")
            }
            return Integer.toHexString(result)
        }

    fun getTerminalExecute(perfilEmvApp: PerfilEmvApp): String {
        val NumberBase = "000000000000"
        val finalNumber = StringBuilder(NumberBase)
        return try {
            finalNumber.substring(
                0,
                NumberBase.length - perfilEmvApp.emvMonedas.limiteOnline.toString().length
            ) + perfilEmvApp.emvMonedas.limiteOnline.toString()
        } catch (exe: Exception) {
            PosLogger.e(TAG, "Error al obtener el terminalExecuteNumber")
            if (perfilEmvApp.perfilesEmv == null) {
                "000000000001"
            } else {
                NumberBase
            }
        }
    }

    enum class TRANSACTION_TYPE(val code: Int) {
        CASH(0x8000), GOODS(0x4000), SERVICES(0x2000), CASHBACK(0x1000), INQUIRY(0x0800), TRANSFER(
            0x0400
        ),
        PAYMENT(0x0200), ADMINISTRATIVE(0x0100), CASH_DEPOSIT(0x0080);

    }

    enum class TERMINAL_DATA_INPUT(val code: Int) {
        NUMERIC_KEYS(0x80), ALPHA_AND_SPECIAL_CHARACTERS(0x40), COMMAND_KEYS(0x20), FUNCTION_KEYS(
            0x10
        );

    }

    enum class DATA_OUTPUT(val code: Int) {
        PRINT_ATTENDANT(0x8000), PRINT_CARDHOLDER(0x4000), DISPLAY_ATTENDANT(0x2000), DISPLAY_CARDHOLDER(0x1000),
        CODE_TABLE_10(0x0200), CODE_TABLE_9(0x0100), CODE_TABLE_8(0x0080), CODE_TABLE_7(0x0040), CODE_TABLE_6(0x0020),
        CODE_TABLE_5(0x0010), CODE_TABLE_4(0x0008), CODE_TABLE_3(0x0004), CODE_TABLE_2(0x0002), CODE_TABLE_1(0x0001);

    }

    /*DECLARAMOS LOS ENUMERADORES PARA LAS CAPABILITIES DEL DONGGLE*/
    enum class CARD_TYPE(private val cardType: Int) {
        BAND(0x40), CHIP(0x20);

        fun cardType(): Int {
            return cardType
        }
    }

    enum class CVM_TYPE(private val cvmtype: Int) {
        PLAINTEXTPINICC(0x80), PINONLINE(0x40), SIGNATURE(0x20), PINOFFLINE(0x10), NOCVM(0x08);

        fun cvmtype(): Int {
            return cvmtype
        }
    }

    enum class SECURITY_TYPE(private val securityType: Int) {
        SDA(0x80), DDA(0x40), CDA(0x08);

        fun securityType(): Int {
            return securityType
        }
    }
}