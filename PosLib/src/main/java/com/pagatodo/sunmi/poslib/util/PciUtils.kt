package com.pagatodo.sunmi.poslib.util

import com.pagatodo.sigmalib.ApiData
import com.pagatodo.sigmalib.EmvManager
import com.pagatodo.sigmalib.emv.CamposPCI
import com.pagatodo.sigmalib.emv.DecodeData
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.model.DataCard
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Operaciones
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.PerfilesEmv
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Productos
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.formulario.*
import net.fullcarga.android.api.oper.TipoOperacion
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object PciUtils {

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

    fun getOperation(operacion: Operaciones): TipoOperacion {
        return when (operacion.operacion) {
            TipoOperacion.PCI_VENTA.tipo -> TipoOperacion.PCI_VENTA
            TipoOperacion.PCI_CONSULTA_X.tipo -> TipoOperacion.PCI_CONSULTA_X
            TipoOperacion.PCI_CONSULTA_Z.tipo -> TipoOperacion.PCI_CONSULTA_Z
            TipoOperacion.PCI_DEVOLUCION.tipo -> TipoOperacion.PCI_DEVOLUCION
            else -> throw Exception("Operación invalida.")
        }
    }

    fun valueForParams(parametros: List<Parametro>, param: CamposPCI): BigDecimal {
        for (entry in parametros) {
            if (param.campo().equals(entry.literal, true)) {
                return AmountUtils.cleanImporteInput(entry.value, AmountUtils.numberFormat)
            }
        }
        return BigDecimal.ZERO
    }

    fun checkAmtBitmap(bitMap: Int, inImporte: Double?, inRetiroEfectivo: Double?, inPropina: Double?, inImpuesto: Double?, inCosto: Double?): Double { //NOSONAR
        var bufferDuble = 0.00
        var intImporteBitmap: String = Integer.toBinaryString(bitMap)
        intImporteBitmap = String.format("%05d", intImporteBitmap.toInt())
        for (lugarBitmap in intImporteBitmap.length downTo 1) {
            when (lugarBitmap) {
                5 -> if (intImporteBitmap[lugarBitmap - 1] == '1') bufferDuble += inImporte!!
                4 -> if (intImporteBitmap[lugarBitmap - 1] == '1') bufferDuble += inRetiroEfectivo!!
                3 -> if (intImporteBitmap[lugarBitmap - 1] == '1') bufferDuble += inPropina!!
                2 -> if (intImporteBitmap[lugarBitmap - 1] == '1') bufferDuble += inImpuesto!!
                1 -> if (intImporteBitmap[lugarBitmap - 1] == '1') bufferDuble += inCosto!!
                else -> bufferDuble += 0.00
            }
        }
        return bufferDuble
    }

    fun isSignature(dataCard: DataCard, perfilesEmv: PerfilesEmv?): Boolean {
        return perfilesEmv?.let {
            it.cvmFirma == 1
                    && (dataCard.entryMode == DataOpTarjeta.PosEntryMode.BANDA || dataCard.entryMode == DataOpTarjeta.PosEntryMode.FALLBACK)
                    && dataCard.pinBlock.isNullOrEmpty()
        } ?: false
    }

    fun validateFallback(perfilesEmv: PerfilesEmv?): Boolean {
        return perfilesEmv?.let { it.chkPermiteFallback == 1 } ?: false
    }

    fun roundAmount(amount: String): String {
        return ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(amount.toBigDecimal().setScale(AmountUtils.numberFormat.maximumFractionDigits, RoundingMode.HALF_EVEN))
    }

    fun createDecodeData(dataCard: DataCard): DecodeData {
        val data = mapOf<String, String>(
            Pair("maskedPAN", dataCard.cardNo),
            Pair("expiryDate", dataCard.expireDate),
            Pair("serviceCode", dataCard.serviceCode),
            Pair("iccdata", dataCard.tlvData),
            Pair("pinBlock", dataCard.pinEncrypt.toString()),
            Pair("encPAN", dataCard.panEncrypt.toString()),
            Pair("cardholderName", dataCard.holderName),
            Pair("encTrack1", dataCard.track1Encrypt.toString()),
            Pair("encTrack2", dataCard.track2Encrypt.toString()),
            Pair("encTrack3", dataCard.track3Encrypt.toString())
        )
        val mapTagsEmv = dataCard.mapTags.apply {
            this as HashMap
            if (!containsKey("9B"))
                this.put("9B", "0000")
        }
        return DecodeData(data, mapTagsEmv)
    }

    private fun getValueFromParameters(parametros: List<Parametro>, param: Parametro): String {
        for (entry in parametros) {
            if (param.literal.equals(entry.literal, true)) {
                return entry.value
            }
        }
        return ""
    }

    fun fillFields(params: List<Parametro>, form: Formulario?): ArrayList<String> {
        val fields = ArrayList<String>()
        form?.apply {
            for (param in form.parametros) {
                if (param.formato.tipo == Formato.Tipo.COSTO) {
                    fields.add(ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(AmountUtils.cleanImporteInput(param.value, AmountUtils.numberFormat)))
                } else if (param.formato.tipo == Formato.Tipo.IMPORTE || param.formato.tipo == Formato.Tipo.IMPORTE_SIN_VAL) {
                    fields.add(ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(AmountUtils.cleanImporteInput(param.value, AmountUtils.numberFormat)))
                } else if (param.formato.tipo == Formato.Tipo.FORMATO_OCULTO ) {//SCA requested pin
                    fields.add((param.formato as FormatoOculto).valor)
                } else if (param.formato.tipo == Formato.Tipo.IMPORTES_EMV ) {//SCA requested pin
                    fields.add((param.formato as FormatoImportesEmv).importe9F02.toPlainString())
                    fields.add((param.formato as FormatoImportesEmv).importe9F03.toPlainString())
                } else if (getValueFromParameters(params, param).isNotEmpty())
                    fields.add(getValueFromParameters(params, param))
            }
        }
        return fields.apply { PosLogger.i("AbstractPciFragment", this.toString()) }
    }

    fun getStringIdTitle(producto: Productos): Int {
        if (producto.tarjetaBanda == 1 && producto.tarjetaEmv == 0 && producto.tarjetaEmvcl == 0)
            return R.string.iniciando_operacion_banda
        if (producto.tarjetaBanda == 0 && producto.tarjetaEmv == 1 && producto.tarjetaEmvcl == 0)
            return R.string.iniciando_operacion_chip
        if (producto.tarjetaBanda == 1 && producto.tarjetaEmv == 1 && producto.tarjetaEmvcl == 0)
            return R.string.iniciando_operacion_banda_chip
        if (producto.tarjetaBanda == 0 && producto.tarjetaEmv == 1 && producto.tarjetaEmvcl == 1)
            return R.string.iniciando_operacion_ctls_chip
        if (producto.tarjetaBanda == 1 && producto.tarjetaEmv == 1 && producto.tarjetaEmvcl == 1)
            return R.string.iniciando_operacion_all
        return -1
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

    private fun finTag(tags: String, tagFind: String): String {
        val index = tags.indexOf(tagFind, ignoreCase = true)
        return if (index != -1) {
            val length = tags.substring(index + tagFind.length, index + tagFind.length + 2).toInt(16)
            tags.substring(index + tagFind.length + 2, index + tagFind.length + 2 + (length * 2))
        } else
            return ""
    }

    fun haveCuotas(perfilesEmv: PerfilesEmv?, pan: String): Boolean {
        perfilesEmv ?: return false
        val rangoCuotas = EmvManager.getRangoCuotas(perfilesEmv.lstCuotasMes)
        if (rangoCuotas.isNotEmpty()) {
            for (cuota in rangoCuotas) {
                if (pan.substring(0, 6).toInt() >= cuota.minbin.toInt() && pan.substring(0, 6).toInt() <= cuota.maxbin.toInt()) {
                    return cuota.cuotasmax > 0 && cuota.cuotasinc > 0
                }
            }
        }
        return false
    }

    @Throws(EmvException::class)
    fun validateDateOfExpiry(perfilesEmv: PerfilesEmv?, expireDate: String) {
        var expireDate = expireDate
        perfilesEmv ?: return
        if (perfilesEmv.chkFechaCaducidad == 1 && expireDate.isNotEmpty()) {
            val simpleDateFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())
            simpleDateFormat.isLenient = false
            try {
                if (expireDate.length == 4) expireDate = "20$expireDate"
                if (simpleDateFormat.parse(expireDate).before(Date())) {
                    throw EmvException(EmvException.ValidationError.ERROR_EXPIRY_DATE)
                }
            } catch (exception: ParseException) {
                PosLogger.e(PosLib.TAG, "Error al Obtener la Fecha de Vencimiento")
            }
        }
    }

    @Throws(EmvException::class)
    fun validateFallback(perfilesEmv: PerfilesEmv?, dataCard: DataCard) {
        perfilesEmv ?: return
        if (perfilesEmv.chkPermiteFallback == 0 && dataCard.entryMode == DataOpTarjeta.PosEntryMode.FALLBACK) {
            throw EmvException(EmvException.ValidationError.ERROR_FALLBACK)
        }
    }

    @Throws(EmvException::class)
    fun validateBinCuotas(perfilesEmv: PerfilesEmv?, pan: String) {
        perfilesEmv ?: return
        if (perfilesEmv.lstCuotasMes < 0) return
        var coincidencia = false
        val rangoCuotas = EmvManager.getRangoCuotas(perfilesEmv.lstCuotasMes)
        if (rangoCuotas.isNotEmpty()) {
            for (cuota in rangoCuotas) {
                if (pan.substring(0, 6).toInt() >= cuota.minbin.toInt() && pan.substring(0, 6).toInt() <= cuota.maxbin.toInt()) {
                    coincidencia = true
                    break
                }
            }
            if (!coincidencia) {
                throw EmvException(EmvException.ValidationError.ERROR_BINES)
            }
        }
    }
}