package com.pagatodo.sunmi.poslib.view

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.pagatodo.sigmalib.ApiData
import com.pagatodo.sigmalib.EmvManager
import com.pagatodo.sigmalib.SigmaBdManager
import com.pagatodo.sigmalib.emv.CamposPCI
import com.pagatodo.sigmalib.emv.PerfilEmvApp
import com.pagatodo.sigmalib.listeners.OnFailureListener
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.SunmiTrxWrapper
import com.pagatodo.sunmi.poslib.config.PinPadConfigV3
import com.pagatodo.sunmi.poslib.harmonizer.SyncService
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.DataCard
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.SyncData
import com.pagatodo.sunmi.poslib.model.TransactionData
import com.pagatodo.sunmi.poslib.setFullScreen
import com.pagatodo.sunmi.poslib.util.*
import com.pagatodo.sunmi.poslib.view.dialogs.*
import com.pagatodo.sunmi.poslib.viewmodel.EmvViewModel
import com.pagatodo.sunmi.poslib.viewmodel.SyncViewModel
import com.squareup.moshi.Moshi
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Operaciones
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.PerfilesEmv
import net.fullcarga.android.api.bd.sigma.generated.tables.pojos.Productos
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import net.fullcarga.android.api.formulario.Formulario
import net.fullcarga.android.api.formulario.Parametro
import net.fullcarga.android.api.oper.TipoOperacion
import java.util.*

abstract class AbstractEmvFragment: Fragment(), SunmiTrxListener<AbstractRespuesta> , OnFailureListener{
    private val verifyCardDialog by lazy { TemporaryDialog.create(requireContext(), PosResult.CardPresentWait) }
    private val requestCardDialog by lazy { RequestCardDialog(requireContext()) }
    private val progressDialog by lazy { DialogProgress(requireContext()) }
    private val viewModelPci by lazy { ViewModelProvider(this)[EmvViewModel::class.java] }
    private val serviceBd by lazy { ViewModelProvider(this)[SyncViewModel::class.java] }
    private val sunmiTransaction by lazy { SunmiTrxWrapper(this) }
    protected lateinit var fullProfile: PerfilEmvApp
    protected lateinit var operacion: Operaciones
    protected lateinit var producto: Productos
    protected var form: Formulario? = null
    private var params = LinkedList<Parametro>()
    private var forceCheckCardType: Int = -1
    private var isAllowCancelEmvProcess = true

    override fun possibleCancelCheckCard(isPossible: Boolean) {
        isAllowCancelEmvProcess = isPossible
    }

    protected fun setDataInit(){
        val dataInitPci = createDataInit()
        operacion = dataInitPci.operacion
        producto = dataInitPci.producto
        form = dataInitPci.form
        fullProfile = EmvManager.getFullPerfil(producto.perfilEmv ?: 0, this)
    }

    protected fun initEmvProcess(params: LinkedList<Parametro>){
        this.params = params
        isAllowCancelEmvProcess = true  //to allow cancelProcessEmv before to requestOnline
        forceCheckCardType = -1
        setDataInit()
        sunmiTransaction.initTransaction()
    }

    override fun onPurchase(dataCard: DataCard) {
        if (validateCard(fullProfile.perfilesEmv, dataCard)) {
            if ((dataCard.entryMode === DataOpTarjeta.PosEntryMode.BANDA ||
                        dataCard.entryMode === DataOpTarjeta.PosEntryMode.FALLBACK) &&
                dataCard.cardNo.startsWith("3") && dataCard.cardNo.length == 15) {
                forwardCidDialog(dataCard)
            } else forwardPymtsDialog(dataCard)
        }
    }

    private fun forwardCidDialog(dataCard: DataCard) {
        val dialogCuotas = CidDialog.newInstance()
        dialogCuotas.setOkListener{
            dataCard.cvv = it.tag.toString().toInt()
            forwardPymtsDialog(dataCard)
        }
        dialogCuotas.isCancelable = false
        dialogCuotas.setCancelListener{
            sunmiTransaction.cancelProcess()
        }
        dialogCuotas.show(requireActivity().supportFragmentManager, dialogCuotas.tag)
    }

    override fun onDialogProcessOnline(message: String?, dataCard: DataCard?) {
        val appName = dataCard?.let { PciUtils.createDecodeData(it).getCamposbydigit(1) } ?: ""
        GlobalScope.launch(Dispatchers.Main) { //to fast UI
            progressDialog.setTitle(message ?: appName+"\n"+getString(R.string.authorizing_pci))
            progressDialog.show()
        }
    }

    override fun onDialogRequestCard(message: String?, cardTypes: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            requestCardDialog(message ?: getString(PciUtils.getStringIdTitle(producto)), createTransactionData().totalAmount, cardTypes, fullProfile.perfilesEmv != null)
        }
    }

    override fun onDismissRequestCard() {
        requireActivity().runOnUiThread {
            isAllowCancelEmvProcess = false
            if (requestCardDialog.dismissable) requestCardDialog.dismiss()
        }
    }

    override fun onDismissRequestOnline() {
        GlobalScope.launch(Dispatchers.Main) {
            progressDialog.dismiss()
        }
    }

    override fun onFailureEmv(error: PosResult, todo: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            TemporaryDialog.create(requireContext(), error).show {
                requireActivity().setFullScreen()
                todo(it)
            }
        }
    }

    override fun onFailureOnline(error: PosResult, doContinue: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            TemporaryDialog.create(requireContext(), error, TemporaryDialog.LONG_SHOW).show {
                requireActivity().setFullScreen()
                if(error == PosResult.NextOperation){
                    if(producto.tarjetaBanda == 0 && producto.tarjetaEmvcl == 0 && producto.tarjetaEmv == 1)
                        doContinue("Pase por contacto.")
                    if(producto.tarjetaBanda == 0 && producto.tarjetaEmvcl == 1 && producto.tarjetaEmv == 0)
                        doContinue("Vuelva a Presentar la Tarjeta.")
                    else doContinue(error.tile)
                } else doContinue(error.tile)
            }
        }
    }

    override fun onSuccessOnline(doContinue: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            TemporaryDialog.create(requireContext(), PosResult.OnlineApproved).show {
                doContinue()
            }
        }
    }

    override fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3) {
        GlobalScope.launch(Dispatchers.Main) {
            delay(300L)
            val pinPadDialog = PinPadDialog.createInstance(pinPadConfig)
            pinPadDialog.setPasswordLength(getPinLength())
            pinPadDialog.setTextAccept("Aceptar")
            pinPadDialog.setTextCancel("Cancelar")
            pinPadDialog.setPinPadListenerV2(pinPadListener)
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction.add(android.R.id.content, pinPadDialog, pinPadDialog.tag).commit()
        }
    }

    override fun onShowSelectApp(listEmvApps: List<String>, appSelect: (Int) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val selectAppEmvDialog = SelectEmvAppDialog()
            selectAppEmvDialog.setAplicaciones(listEmvApps, appSelect)
            selectAppEmvDialog.show(requireActivity().supportFragmentManager, selectAppEmvDialog.tag)
        }
    }

    private fun forwardPymtsDialog(dataCard: DataCard) {
        if (PciUtils.haveCuotas(fullProfile.perfilesEmv, dataCard.cardNo)) {
            val dialogCuotas = DialogPayments.newInstance(fullProfile.perfilesEmv)
            dialogCuotas.setCuotasListener{
                saveTmpDataSync(getDataSync(dataCard)) {
                    onDialogProcessOnline(dataCard = dataCard)
                    dataCard.monthlyPayments = it.tag as Int
                    viewModelPci.executeEmvOpr(PciUtils.getOperation(operacion), producto.codigo, PciUtils.fillFields(params, form), createDataOpTarjeta(dataCard))
                }
            }
            dialogCuotas.isCancelable = false
            dialogCuotas.setCancelListener {
                sunmiTransaction.cancelProcess()
            }
            dialogCuotas.show(requireActivity().supportFragmentManager, dialogCuotas.tag)
        } else {
            saveTmpDataSync(getDataSync(dataCard)) {
                onDialogProcessOnline(dataCard = dataCard)
                viewModelPci.executeEmvOpr(PciUtils.getOperation(operacion), producto.codigo, PciUtils.fillFields(params, form), createDataOpTarjeta(dataCard))
            }
        }
    }
    private fun saveTmpDataSync(syncData: SyncData, doContinue: () -> Unit){
        val moshi = Moshi.Builder().build()
        serviceBd.insertSyncData(Sync(dateTime= Date(), status = StatusTrx.PROGRESS.name, data = moshi.adapter(SyncData::class.java).toJson(syncData)))
        doContinue()
    }

    override fun getVmodelPCI() = viewModelPci

    private fun requestCardDialog(message: String?, formatedAmount: String, cardTypes: Int = 0, showAmount:Boolean = true) {
        requestCardDialog.mensaje = message
        requestCardDialog.amount = formatedAmount
        requestCardDialog.isPmx = operacion.producto == "PEMEX001"
        requestCardDialog.showRfReading = (cardTypes >= AidlConstants.CardType.NFC.value)
        requestCardDialog.showAmt = showAmount
        requestCardDialog.setOnDismissListener {
            if (isAllowCancelEmvProcess) {
                sunmiTransaction.cancelProcess()
            }
        }
        requestCardDialog.show()
    }

    override fun showRemoveCard(dataCard: DataCard?) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!verifyCardDialog.isShowing) {
                verifyCardDialog.show {
                    requireActivity().setFullScreen()
                }
            }
        }
    }

    override fun checkCardTypes(): Int {
        var cardType: Int? = null
        if (producto.tarjetaBanda == 1)//Banda
            cardType = AidlConstants.CardType.MAGNETIC.value
        if (producto.tarjetaEmv == 1)//Chip
            cardType = cardType?.let { it or AidlConstants.CardType.IC.value }
                ?: run { AidlConstants.CardType.IC.value }
        if (producto.tarjetaEmvcl == 1)//Ctls
            cardType = cardType?.let { it or AidlConstants.CardType.NFC.value }
                ?: run { AidlConstants.CardType.NFC.value }
        return cardType
            ?: run { throw RuntimeException("No se han agregado operaciones para pais.") }
    }

    fun createDataOpTarjeta(dataCard: DataCard): DataOpTarjeta {
        return DataOpTarjeta(
            dataCard.panEncrypt,
            dataCard.track1Encrypt,
            dataCard.track2Encrypt,
            dataCard.track3Encrypt,
            dataCard.icDataEncrypt,
            dataCard.pinEncrypt,
            dataCard.entryMode,
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().totalAmount),
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().cashBackLector),
            createTransactionData().terminalParams.currencyCode,
            createTransactionData().decimals,
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().amount),
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().cashBackAmount),
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().gratuity),
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().taxes),
            ApiData.APIDATA.datosSesion.datosTPV.convertirImporte(createTransactionData().comisions),
            dataCard.monthlyPayments,
            dataCard.daysDeferred,
            createTransactionData().zipCode,
            dataCard.cvv
        )
    }

    override fun createTransactionData(): TransactionData {
        val inImporte = PciUtils.valueForParams(params, CamposPCI.IMPORTE).toDouble()
        val inImpuesto = PciUtils.valueForParams(params, CamposPCI.IMPUESTO).toDouble()
        val inPropina = PciUtils.valueForParams(params, CamposPCI.PROPINA).toDouble()
        val inRetiroEfectivo = PciUtils.valueForParams(params, CamposPCI.CASHBACK).toDouble()
        val inCosto = fullProfile.perfilesEmv?.costo?.toDouble() ?: 0.0

        val totalAmt = fullProfile.perfilesEmv?.let { PciUtils.checkAmtBitmap(it.importeBitmap, inImporte, inRetiroEfectivo, inPropina, inImpuesto, inCosto) } ?: inImporte
        val cashBackAmtLector = fullProfile.perfilesEmv?.let { PciUtils.checkAmtBitmap(it.importe2Bitmap, inImporte, inRetiroEfectivo, inPropina, inImpuesto, inCosto) } ?: 0.0

        return TransactionData().apply {
            this.totalAmount = PciUtils.roundAmount(totalAmt.toString())
            this.transType = if ((operacion.operacion ?: "") == TipoOperacion.PCI_VENTA.tipo) Constants.TransType.PURCHASE else Constants.TransType.REFUND
            this.cashBackAmount = ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(inRetiroEfectivo.toString())
            this.decimals = fullProfile.emvMonedas?.decimales ?: 0
            this.amount = ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(inImporte.toString())
            this.gratuity = ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(inPropina.toString())
            this.taxes = ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(inImpuesto.toString())
            this.comisions = ApiData.APIDATA.datosSesion.datosTPV.rellenarImporte(inCosto.toString())
            this.sigmaOperation = operacion.operacion
            this.tagsEmv = producto.perfilEmv?.let { PciUtils.orderTags(EmvManager.getTagsPerfil(it)) } ?: LinkedList()
            this.terminalParams = createParamV2()
            this.cashBackLector = PciUtils.roundAmount(cashBackAmtLector.toString())
        }
    }

    override fun onShowSingDialog(ref: String, doContinue: (ByteArray) -> Unit) {
        val listener = object : SingDialog.SignatureListener {
            override fun onSingSuccess(singBytes: ByteArray) {
                doContinue(singBytes)
            }

            override fun onCancel() {
                TemporaryDialog.create(requireContext(), PosResult.ErrorEmptySing).show {
                    onShowSingDialog(ref, doContinue)
                }
            }
        }
        val firmaDialogo = SingDialog.newInstance(listener, ref)
        firmaDialogo.isCancelable = false
        firmaDialogo.show(requireActivity().supportFragmentManager, firmaDialogo.tag)
    }

    override fun createParamV2() = EmvTermParamV2().apply {
        capability = UtilCapabilities.terminalCapabilitiesCode(fullProfile)
        addCapability = UtilCapabilities.additionalTerminalCapabilitiesCode()
        currencyCode = String.format("%04d", fullProfile.perfilesEmv?.let { fullProfile.emvMonedas.codigoMoneda.toInt() } ?: 0)
        countryCode = ApiData.APIDATA.paisCode
        currencyExp = "0${fullProfile.emvMonedas?.exponente ?: "0"}"
        TTQ = UtilCapabilities.createTTQ(fullProfile)
    }

    override fun doOperationNext(nextOperation: OperacionSiguiente, message: String, doContinue: (String) -> Unit) {
        onDismissRequestOnline()
        producto = SigmaBdManager.getProductoxId(nextOperation.procodIdNext, this)
        operacion = SigmaBdManager.getOperacionPorProducto(producto.codigo, nextOperation.tipoOperacionNext.tipo, this)
        form = nextOperation.formularioNext
        if (producto.tarjetaBanda == 0 && producto.tarjetaEmvcl == 0 && producto.tarjetaEmv == 0){
            TemporaryDialog.create(requireContext(), PosResult.ErrorEmptyPin).show {
                requireActivity().setFullScreen()
                sunmiTransaction.getPin()
            }
        } else doContinue(message)
    }

    override fun onSync(dataCard: DataCard) {
        /*GlobalScope.launch(Dispatchers.Main) {
            viewModelPci.executeSync(producto.codigo, PciUtils.fillFields(params, form),
                createDataOpTarjeta(dataCard), ApiData.APIDATA.datosSesion.datosTPV.stanProvider.ultimo)
        }*/

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncWorker: WorkRequest = OneTimeWorkRequestBuilder<SyncService>().setConstraints(constraints).build()
        val workManager = WorkManager.getInstance(requireContext())

        workManager.enqueue(syncWorker)

        workManager.getWorkInfoByIdLiveData(syncWorker.id).observe(requireActivity()) { info ->
            when (info.state){
                WorkInfo.State.SUCCEEDED -> {
                    PosLogger.d(PosLib.TAG, "Success")
                    onDismissRequestOnline()
                    sunmiTransaction.onFailure(PosResult.SyncOperationSuccess)
                }
                WorkInfo.State.FAILED -> {
                    if(info.outputData.getString(SyncService.KEY_MESSAGE) == "La operacion esta anulada")
                        sunmiTransaction.onFailure(PosResult.SyncOperationSuccess)
                    else
                        sunmiTransaction.onFailure(PosResult.SyncOperationFailed)
                }
                else -> {sunmiTransaction.onFailure(PosResult.SyncOperationFailed)}
            }
        }
    }

    private fun getDataSync(dataCard: DataCard) = SyncData(
        producto.codigo, PciUtils.fillFields(params, form),
        createDataOpTarjeta(dataCard), ApiData.APIDATA.datosSesion.datosTPV.stanProvider.ultimo
    )

    override fun pinMustBeForced() = operacion.pin == 2

    override fun isPossibleFallback() = PciUtils.validateFallback(fullProfile.perfilesEmv)

    override fun requireSignature(dataCard: DataCard) =
        PciUtils.isSignature(dataCard, fullProfile.perfilesEmv)

    override fun showReading() {
        GlobalScope.launch(Dispatchers.Main) {
            requestCardDialog.setMessage("Leyendo...")
            requestCardDialog.reading()
        }
    }

    override fun onFailure(throwable: Throwable?) {
        TemporaryDialog.create(requireContext(), PosResult.Generic.apply { tile = throwable?.message ?: "" })
    }

    private fun validateCard(perfilesEmv: PerfilesEmv?, dataCard: DataCard): Boolean {
        return try {
            PciUtils.validateFallback(perfilesEmv, dataCard)
            PciUtils.validateDateOfExpiry(perfilesEmv, dataCard.expireDate)
            PciUtils.validateBinCuotas(perfilesEmv, dataCard.cardNo)
            true
        } catch (exception: EmvException) {
            val resultError = PosResult.Generic
            resultError.tile = exception.message!!
            sunmiTransaction.onFailure(resultError)
            sunmiTransaction.cancelProcess()
            false
        }
    }

    override fun onShowZipDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    override fun onShowDniDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    abstract fun createDataInit(): DataInitPci

    protected fun onUpdateSyncData() {
        serviceBd.deleteAll()
    }

    data class DataInitPci(val producto: Productos, val operacion :Operaciones, val form : Formulario?)
}