package com.pagatodo.sunmi.poslib

import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import com.pagatodo.sunmi.poslib.config.PinPadConfigV3
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.model.*
import com.pagatodo.sunmi.poslib.util.*
import com.pagatodo.sunmi.poslib.util.Constants.DEVOLUCION
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.EMVCandidateV2
import com.sunmi.pay.hardware.aidlv2.emv.EMVListenerV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.DataOpTarjeta
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class SunmiTransaction {
    private var hexStrPin: ByteArray = ByteArray(0)
    private var isRequestPin = false
    private var mAppSelect = 0
    private var customMessage: String? = null
    protected var isRequestSignature = false
    protected var emvTags: HashMap<String, String> = HashMap()
    protected var mCardType: AidlConstants.CardType = AidlConstants.CardType.MAGNETIC
    protected var allowFallback = false
    protected var mPinType: Int = 0 // 0-online pin, 1-offline pin
    private var mCardNo: String = ""
        get() {
            if (field.isEmpty())
                field = cardNoFromKernel
            return field
        }

    fun setTerminalParams() = posInstance().mEMVOptV2?.apply {
        EmvUtil.setTerminalParam(getTransactionData().terminalParams, this)
    }

    fun startPayProcess() = try {
        val amount = setDecimalsAmount(getTransactionData().amount)
        posInstance().mEMVOptV2?.initEmvProcess()// Before check card, initialize emv process(clear all TLV)
        if (amount.toLong() > 0 || getTransactionData().transType == Constants.TransType.REFUND) {
            checkCard()
        } else throw IllegalArgumentException("Amount must be above zero.")
    } catch (e: Exception) {
        e.printStackTrace()
    }

    fun tryAgain() = checkCard()

    protected fun getPin(dataCard: DataCard) {
        PosLogger.e(PosLib.TAG, "getPin (dataCard != null required): $dataCard")
        initPinPad(dataCard)
    }

    fun finishOnlineProcessStatus(tlvString: String? = null, tlvResponse: Constants.TlvResponses, message: String? = null) = try {//(CSU)Card Update Status
        val tlvMap = TLVUtil.buildTLVMap(tlvString ?: tlvResponse.response)
        customMessage = message
        val tags = LinkedList<String>()
        val values = LinkedList<String>()
        val tagsAccept = arrayOf("71", "72", "91", "8A", "89")

        for (tag in tagsAccept) {
            tags.add(tag)
            values.add(tlvMap[tag]?.value ?: "")
        }

        val out = ByteArray(1024)
        val len = posInstance().mEMVOptV2?.importOnlineProcStatus(
            tlvResponse.status,
            tags.toTypedArray(),
            values.toTypedArray(),
            out
        )
        PosLogger.e(PosLib.TAG, "card update status code::$len")
        len?.also {  //Validar si esto aplica para MTIP 2.60 Refund
            if ((it == PosResult.DoSyncOperation.code || it == PosResult.TransRefused.code) && tlvResponse.status == 0) {
                onFailureTrx(PosResult.DoSyncOperation)
                customMessage = PosResult.DoSyncOperation.message
            } else if (mCardType == AidlConstants.CardType.MAGNETIC && tlvResponse.status == 0)
                onApprovedTrx()
            else if (mCardType == AidlConstants.CardType.MAGNETIC)
                onFailureTrx(getPosResult(AidlConstants.CardType.MAGNETIC.value, customMessage))
        }
    } catch (exe: Exception) {
        PosLogger.e(PosLib.TAG, exe.message)
    }

    private fun checkCard() = try {
        posInstance().mReadCardOptV2?.checkCard(getCheckCardType(), mCheckCardCallback, 160)
    } catch (exe: Exception) {
        PosLogger.e(PosLib.TAG, exe.message)
        cancelProcessEmv()
    }

    fun cancelProcessEmv() = try {
        posInstance().mReadCardOptV2?.cardOff(mCardType.value)
        posInstance().mReadCardOptV2?.cancelCheckCard()
    } catch (exe: Exception) {
        PosLogger.e(PosLib.TAG, exe.message)
    }

    private fun transactProcess() = try {
        val bundle = Bundle()
        bundle.putString("amount", setDecimalsAmount(getTransactionData().amount))
        bundle.putString("transType", getTransactionData().transType.type)
        bundle.putInt("flowType", AidlConstants.EMV.FlowType.TYPE_EMV_STANDARD)
        bundle.putInt("cardType", mCardType.value)
        posInstance().mEMVOptV2?.transactProcessEx(bundle, mEMVListener)
    } catch (exe: Exception) {
        PosLogger.e(PosLib.TAG, exe.message)
        cancelProcessEmv()
    }

    private fun getDataCard(mapTags: Map<String, TLV?>): DataCard { //NOSONAR
        val dataCard = EmvUtil.parseTrack2(mapTags["57"]?.value)
        dataCard.cardNo =
            if (dataCard.cardNo.isNullOrEmpty()) mapTags[Constants.TagsEmv.ENC_PAN.tag]?.value else dataCard.cardNo
        dataCard.track1 = mapTags[Constants.TagsEmv.ENC_TRACK_1.tag]?.value ?: ""
        dataCard.track2 = mapTags[Constants.TagsEmv.ENC_TRACK_2.tag]?.value ?: ""
        dataCard.track3 = mapTags[Constants.TagsEmv.ENC_TRACK_3.tag]?.value ?: ""
        dataCard.holderName = mapTags[Constants.TagsEmv.CARDHOLDER_NAME.tag]?.value ?: ""
        dataCard.tlvData = getHexEmvTags(mapTags)
        dataCard.pinBlock = ByteUtil.bytes2HexStr(hexStrPin)
        dataCard.entryMode =
            if (mCardType == AidlConstants.CardType.NFC) DataOpTarjeta.PosEntryMode.CONTACLESS else DataOpTarjeta.PosEntryMode.CHIP
        dataCard.mapTags = emvTags
        return dataCard.apply { cardNo = Regex("[^0-9 ]").replace(cardNo, "") }
    }

    private fun getDataCard(bundleTags: Bundle): DataCard { //NOSONAR
        val track1 = bundleTags.getString(Constants.track1) ?: ""
        val track2 = bundleTags.getString(Constants.track2) ?: ""
        val track3 = bundleTags.getString(Constants.track3) ?: ""
        val name = track1.substring(track1.indexOf("^") + 1)
        val cardHolderName = name.substring(0, name.indexOf("^"))
        var serviceCode = ""
        track2.apply {
            val index = indexOf("=")
            if (index != -1) {
                mCardNo = substring(0, index)
                serviceCode = substring(index + 5, index + 8)
            }
        }
        return DataCard().apply {
            this.cardNo = track1.run { substring(indexOf('B') + 1, indexOf('^')) }
            this.track1 = "%$track1?"
            this.track2 = ";$track2?"
            this.track3 = track3
            this.holderName = cardHolderName
            this.serviceCode = serviceCode
            this.pinBlock = ByteUtil.bytes2HexStr(hexStrPin)
            this.expireDate = name.substring(name.indexOf("^")).substring(1, 5)
            PosLogger.e(PosLib.TAG, toString())
        }
    }

    private fun setDecimalsAmount(amount: String): String {
        if (getTransactionData().decimals == 0 && amount.isNotEmpty()) {
            return amount.plus("00")
        }
        return amount
    }

    private val mCheckCardCallback: CheckCardCallbackV2 = object : CheckCardCallbackV2Wrapper() {
        @Throws(RemoteException::class)
        override fun findMagCard(info: Bundle) {
            PosLogger.e(PosLib.TAG, "info:: $info")
            mCardType = AidlConstants.CardType.MAGNETIC
            try {
                val dataCard = getDataCard(info)
                dataCard.entryMode =
                    if (allowFallback) DataOpTarjeta.PosEntryMode.FALLBACK else DataOpTarjeta.PosEntryMode.BANDA
                if (EmvUtil.isChipCard(dataCard.serviceCode) && !allowFallback) { //Tarjeta por chip no fallback
                    GlobalScope.launch(Dispatchers.Main) { onFailureTrx(PosResult.CardDenial) }
                    cancelProcessEmv()
                } else if (pinMustBeForced() || EmvUtil.requiredNip(dataCard.serviceCode))
                    initPinPad(dataCard)
                else
                    goOnlineProcess(dataCard)
            } catch (e: Exception) {
                PosLogger.e(PosLib.TAG, e.toString())
                GlobalScope.launch(Dispatchers.Main) { onFailureTrx(PosResult.ErrorCheckCard) }
            }
        }

        @Throws(RemoteException::class)
        override fun findICCard(atr: String) {
            PosLogger.e(PosLib.TAG, "findICCard atr:: $atr")
            mCardType = AidlConstants.CardType.IC
            transactProcess()
        }

        @Throws(RemoteException::class)
        override fun findRFCard(uuid: String) {
            PosLogger.e(PosLib.TAG, "uuid:: $uuid")
            mCardType = AidlConstants.CardType.NFC
            readingCard()
            transactProcess()
        }

        @Throws(RemoteException::class)
        override fun onError(code: Int, message: String) {
            PosLogger.e(PosLib.TAG, "onError::$code message:: $message")
            GlobalScope.launch(Dispatchers.Main) {
                onFailureTrx(getPosResult(code, PosResult.ErrorCheckCard.message))
            }
        }
    }

    private fun tagsTlvToTagsString(mapTags: Map<String, TLV>) = HashMap<String, String>().apply {
        for ((key, value) in mapTags) {
            this[key] = value.value
        }
    }

    private val mEMVListener: EMVListenerV2 = object : EMVListenerV2.Stub() {
        @Throws(RemoteException::class)
        override fun onWaitAppSelect(list: List<EMVCandidateV2>, b: Boolean) {
            val candidateNames = getCandidateNames(list)
            PosLogger.e(PosLib.TAG, "onWaitAppSelect b->$b candidateNames: $candidateNames")
            val appEmv = object : AppEmvSelectListener {
                override fun onAppEmvSelected(position: Int) {
                    try {
                        PosLogger.e(PosLib.TAG, "onAppEmvSelected pos: $position")
                        posInstance().mEMVOptV2?.importAppSelect(position)
                    } catch (exe: RemoteException) {
                        onFailureTrx(PosResult.ErrorSelectApp)
                    }
                }
            }
            onSelectEmvApp(candidateNames, appEmv)
        }

        @Throws(RemoteException::class)
        override fun onAppFinalSelect(appSelected: String) {
            PosLogger.e(PosLib.TAG, "tag9F06Value:: $appSelected")
            if (appSelected.isNotEmpty()) {
                val isVisa = appSelected.startsWith("A000000003")
                val isMaster = appSelected.startsWith("A000000004")
                val isUnion = appSelected.startsWith("A000000333")
                mAppSelect = when {
                    isUnion -> 0
                    isVisa -> 1
                    isMaster -> {// MasterCard(PayPass)
                        val configId =
                            if (getTransactionData().transType == Constants.TransType.REFUND)
                                DEVOLUCION
                            else
                                appSelected.substring(0, 13)
                        setPayPassConfig(configId)
                        2
                    }
                    else -> -1
                }
                PosLogger.e(PosLib.TAG, "detect $mAppSelect card")
                posInstance().mEMVOptV2?.importAppFinalSelectStatus(0)
            }
        }

        @Throws(RemoteException::class)
        override fun onConfirmCardNo(cardNo: String) {
            PosLogger.e(PosLib.TAG, cardNo)
            mCardNo = cardNo
            posInstance().mEMVOptV2?.importCardNoStatus(0)
        }

        @Throws(RemoteException::class)
        override fun onRequestShowPinPad(pinType: Int, remainTime: Int) {
            PosLogger.e(PosLib.TAG, "pinType::$pinType, remainTime::$remainTime")
            mPinType = pinType
            isRequestPin = true
            if(remainTime == -1)
                initPinPad()
            else
                initPinPad(messageError = PosResult.NoSecretWrong.message + ", 3 Intento(s) Restante(s).")
        }

        @Throws(RemoteException::class)
        override fun onRequestSignature() {
            PosLogger.e(PosLib.TAG, "onRequestSignature")
            isRequestSignature = true
            posInstance().mEMVOptV2?.importSignatureStatus(0)
        }

        @Throws(RemoteException::class)
        override fun onOnlineProc() {
            PosLogger.e(PosLib.TAG, "::")
            val mapTags = tlvData
            emvTags = tagsTlvToTagsString(mapTags)
            val dataCard = getDataCard(mapTags)
            if (!isRequestPin && pinMustBeForced())
                initPinPad(dataCard)
            else if (mCardType == AidlConstants.CardType.NFC)
                checkAndRemoveCard(dataCard)
            else
                goOnlineProcess(dataCard)
        }

        override fun onCertVerify(p0: Int, p1: String?) {
            PosLogger.e(PosLib.TAG, "p0-> $p0 p1-> $p1")
        }

        @Throws(RemoteException::class)
        override fun onCardDataExchangeComplete() {
            PosLogger.e(PosLib.TAG, "onCardDataExchangeComplete")
        }

        @Throws(RemoteException::class)
        override fun onTransResult(code: Int, desc: String?) { //when has finalized process online
            PosLogger.e(PosLib.TAG, "code: $code desc: $desc")
            if (getTransactionData().transType == Constants.TransType.REFUND && code == PosResult.TransTerminate.code) {//transResult does not matter when transaction is a refund
                onOnlineProc()
            } else if (code == PosResult.CardAbsentAproved.code)
                checkAndRemoveCard()
            else GlobalScope.launch(Dispatchers.Main) {
                customMessage?.apply {
                    if (this != PosResult.DoSyncOperation.message)
                        onFailureTrx(getPosResult(code, this))
                } ?: onFailureTrx(getPosResult(code, desc))
            }
        }

        @Throws(RemoteException::class)
        override fun onConfirmationCodeVerified() { //Only confirmation phone required
            val outData = ByteArray(512)
            var strOutcomeMessage = ""
            posInstance().mEMVOptV2?.getTlv(AidlConstants.EMV.TLVOpCode.OP_PAYPASS, "DF8129", outData)?.apply {
                if (this > 0) {
                    val data = ByteArray(this)
                    System.arraycopy(outData, 0, data, 0, this)
                    strOutcomeMessage = ByteUtil.bytes2HexStr(data)
                    PosLogger.e(PosLib.TAG, "onRequestDataExchange DF8129:: $strOutcomeMessage")
                }
            }
            posInstance().mReadCardOptV2?.cardOff(mCardType.value) // card off
            GlobalScope.launch(Dispatchers.Main) { onSeePhone(PosResult.SeePhone.message) }
        }

        @Throws(RemoteException::class)
        override fun onRequestDataExchange(s: String) {
            PosLogger.e(PosLib.TAG, "onRequestDataExchange s: $s")
        }
    }

    private val cardNoFromKernel: String
        get() {//get card num from tags
            try {
                val tagList = arrayOf("57", "5A")
                val outData = ByteArray(256)
                posInstance().mEMVOptV2?.apply {
                    val len = getTlvList(AidlConstants.EMV.TLVOpCode.OP_NORMAL, tagList, outData)
                    if (len <= 0) {
                        PosLogger.e(PosLib.TAG, "error, code::$len"); return ""
                    }
                    val bytes = outData.copyOf(len)
                    val tlvMap = TLVUtil.buildTLVMap(bytes)
                    return tlvMap["57"]?.let {
                        EmvUtil.parseTrack2(it.value).cardNo
                    } ?: run {
                        tlvMap["5A"]?.value ?: ""
                    }
                } ?: PosLogger.e(PosLib.TAG, "EMVOptV2 is null")
            } catch (e: RemoteException) {
                e.printStackTrace()
                PosLogger.e(PosLib.TAG, e.message)
            }
            return ""
        }

    private fun setPayPassConfig(configId: String) = try {// set PayPass(MasterCard) tlv data
        val posCfg = posInstance().posConfig.paypassConfig.getConfig(configId)
        val tagsPayPass = arrayOf("DF8117", "DF8118", "DF8119", "DF811F", "DF811E", "DF812C", "DF8123", "DF8124", "DF8125", "DF8126", "DF811B", "DF811D", "DF8122", "DF8120", "DF8121")
        val valuesPayPass = arrayOf("60", posCfg.cvmCapability, "08", "C8", "00", "00", posCfg.floorLimit, posCfg.termClssLmt, posCfg.termClssLmt, posCfg.cvmLmt, "B0", "02", posCfg.tACOnline, posCfg.tACDefault, posCfg.tACDenial)
        posInstance().mEMVOptV2?.setTlvList(
            AidlConstants.EMV.TLVOpCode.OP_PAYPASS,
            tagsPayPass,
            valuesPayPass
        )
    } catch (e: RemoteException) {
        PosLogger.e(PosLib.TAG, e.message)
    }

    fun cancelOperationWithMessage() = try {
        cancelProcessEmv()
        onFailureTrx(PosResult.OperationCanceled)
    } catch (exe: Exception) {
        onFailureTrx(PosResult.OperationCanceled)
    }

    private fun getHexEmvTags(mapTags: Map<String, TLV?>) = StringBuilder().apply {
        for (tag in mapTags.keys)
            append(mapTags[tag]?.recoverToHexStr())
    }.toString()

    protected fun clearVars() {
        mCardNo = ""
        allowFallback = false
        isRequestSignature = false
        isRequestPin = false
        hexStrPin = ByteArray(0)
        customMessage = null
    }

    private fun getCandidateNames(candiList: List<EMVCandidateV2>): List<String> {
        val appsName: MutableList<String> = ArrayList()
        if (candiList.isNullOrEmpty()) return appsName
        for (i in candiList.indices) {
            val candi = candiList[i]
            var name = candi.appLabel
            name = if (TextUtils.isEmpty(name)) candi.appPreName else name
            name = if (TextUtils.isEmpty(name)) candi.appName else name
            name = if (TextUtils.isEmpty(name)) "" else name
            appsName.add(name)
        }
        return appsName
    }

    private val tlvData: Map<String, TLV>
        get() = try {
            val tagList = if (getTransactionData().tagsEmv.isNotEmpty())
                getTransactionData().tagsEmv.toTypedArray()
            else
                EmvUtil.tagsDefault
            PosLogger.e(PosLib.TAG, "tagList to get-> ${tagList.toList()}")
            val outData = ByteArray(4096)
            val map: MutableMap<String, TLV> = LinkedHashMap()
            val tlvOpCode: Int = if (AidlConstants.CardType.NFC == mCardType) {
                when (mAppSelect) {
                    1 -> AidlConstants.EMV.TLVOpCode.OP_PAYWAVE
                    2 -> AidlConstants.EMV.TLVOpCode.OP_PAYPASS
                    else -> AidlConstants.EMV.TLVOpCode.OP_NORMAL
                }
            } else {
                AidlConstants.EMV.TLVOpCode.OP_NORMAL
            }
            posInstance().mEMVOptV2?.getTlvList(AidlConstants.EMV.TLVOpCode.OP_NORMAL, tagList, outData)?.let {
                if (it > 0) {
                    val bytes = outData.copyOf(it)
                    val hexStr = ByteUtil.bytes2HexStr(bytes)
                    val tlvMap = TLVUtil.buildTLVMap(hexStr)
                    map.putAll(tlvMap)
                }
            }
            //Contacless tags
            posInstance().mEMVOptV2?.getTlvList(tlvOpCode, EmvUtil.payPassTags, outData)?.let {
                if (it > 0) {
                    val bytes = outData.copyOf(it)
                    val hexStr = ByteUtil.bytes2HexStr(bytes)
                    val tlvMap = TLVUtil.buildTLVMap(hexStr)
                    tlvMap["9F6E"]?.apply { map["9F6E"] = this }
                }
            }
            cleanMap(map)
        } catch (exe: Exception) {
            PosLogger.e(PosLib.TAG, exe.message)
            cancelOperationWithMessage()
            emptyMap()
        }

    private fun cleanMap(mapTags: MutableMap<String, TLV>) = mapTags.let {//remove empty tags
        for ((key, tlv) in HashMap(it)) {
            if (tlv.value.isEmpty())
                it.remove(key)
        }
        it
    }

    private fun initPinPad(dataCard: DataCard? = null, messageError: String? = null) {//dataCard must be different null for a manual pin
        try {
            val pinPadConfig = PinPadConfigV3()
            val panBytes = mCardNo.substring(mCardNo.length - 13, mCardNo.length - 1).toByteArray(charset("US-ASCII"))
            pinPadConfig.pinPadType = 1 //0: Default 1:Custom
            pinPadConfig.pinType = mPinType
            pinPadConfig.isOrderNumKey = true
            pinPadConfig.pan = panBytes
            pinPadConfig.timeout = 60 * 1000 // input password timeout
            pinPadConfig.pinKeyIndex = 11 // pik index
            pinPadConfig.maxInput = 12
            pinPadConfig.minInput = 4
            pinPadConfig.keySystem = 0
            pinPadConfig.algorithmType = 0
            pinPadConfig.informError = messageError

            onShowPinPad(object : PinPadListenerV2.Stub() {
                override fun onPinLength(len: Int) {
                    //Empty method
                }

                override fun onConfirm(i: Int, pinBlock: ByteArray?) {
                    PosLogger.e(PosLib.TAG, "i:: $i pinBlock:: $pinBlock")
                    try {
                        pinBlock?.also {
                            hexStrPin = pinBlock
                            dataCard?.apply {
                                this.pinBlock = if(mPinType == PinTypes.PIN_ONLINE.pinValue) ByteUtil.bytes2HexStr(hexStrPin) else null
                                if (mCardType == AidlConstants.CardType.NFC)
                                    checkAndRemoveCard(dataCard)
                                else
                                    goOnlineProcess(this@apply)
                            } ?: posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 0)
                        } ?: run {
                            initPinPad(dataCard, PosResult.ErrorEmptyPin.message)
                        }
                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, exe.message)
                        cancelOperationWithMessage()
                    }
                }

                override fun onCancel() {
                    try {
                        posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 1)
                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, exe.message)
                        cancelOperationWithMessage()
                    }
                }

                override fun onError(code: Int) {
                    try {
                        PosLogger.e(PosLib.TAG, "code::$code")
                        posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 3)
                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, exe.message)
                        cancelOperationWithMessage()
                    }
                }
            }, pinPadConfig)
        } catch (exe: Exception) {
            PosLogger.e(PosLib.TAG, exe.message)
            cancelOperationWithMessage()
        }
    }

    private fun checkAndRemoveCard(dataCard: DataCard? = null) {
        try {//Check and notify remove card
            val status = posInstance().mReadCardOptV2?.getCardExistStatus(mCardType.value)?.also {
                if (it < 0) {
                    onFailureTrx(PosResult.ErrorCheckPresentCard); return
                }
            }
            PosLogger.e(PosLib.TAG, "status::$status")
            when (status) {
                AidlConstants.CardExistStatus.CARD_ABSENT -> {
                    dataCard?.apply {
                        if (mCardType == AidlConstants.CardType.NFC)
                            goOnlineProcess(this)
                    } ?: run { onApprovedTrx() }
                }
                AidlConstants.CardExistStatus.CARD_PRESENT -> {
                    GlobalScope.launch(Dispatchers.Main) { onRemoveCard(dataCard) }
                    posInstance().mBasicOptV2?.buzzerOnDevice(1, 2750, 200, 0)
                    loopRemoveCard(dataCard)
                }
                else -> throw IllegalArgumentException("Unknown status $status.")
            }
        } catch (e: Exception) {
            PosLogger.e(PosLib.TAG, e.message)
        }
    }

    private fun loopRemoveCard(dataCard: DataCard? = null) = GlobalScope.launch(Dispatchers.Main) {
        delay(500)
        checkAndRemoveCard(dataCard)
    }

    protected abstract fun goOnlineProcess(dataCard: DataCard)

    abstract fun onFailureTrx(result: PosResult)

    abstract fun onSeePhone(outcomeMessage: String)

    abstract fun onApprovedTrx()

    abstract fun getCheckCardType(): Int

    abstract fun pinMustBeForced(): Boolean

    abstract fun readingCard()

    abstract fun onShowPinPad(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV3)

    abstract fun onSelectEmvApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener)

    abstract fun getTransactionData(): TransactionData

    abstract fun onRemoveCard(dataCard: DataCard?)
}