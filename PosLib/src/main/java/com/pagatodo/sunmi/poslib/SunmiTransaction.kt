package com.pagatodo.sunmi.poslib

import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.model.*
import com.pagatodo.sunmi.poslib.util.*
import com.pagatodo.sunmi.poslib.util.Constants.DEVOLUCION
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.EMVCandidateV2
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.emv.EMVListenerV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class SunmiTransaction {
    private var hexStrPin: ByteArray = ByteArray(0)
    private var isRequestPin = false
    private var customMessage: String? = null
    protected var isRequestSignature = false
    protected var emvTags: HashMap<String, String> = HashMap()
    protected var mCardType: AidlConstants.CardType = AidlConstants.CardType.MAGNETIC
    protected var allowFallback = false
    protected var mPinType: Int = 0 // 0-online pin, 1-offline pin
    private var mCardNo: String = ""
        get() {
            if (field.isEmpty()) {
                field = cardNoFromKernel
            }
            return field
        }

    fun setTerminalParams() = posInstance().mEMVOptV2?.apply {
        EmvUtil.setTerminalParam(getTransactionData().terminalParams, this)
    }

    fun startPayProcess() = try {
        clearVars()
        val amount = setDecimalsAmount(getTransactionData().amount)
        posInstance().mEMVOptV2?.initEmvProcess()// Before check card, initialize emv process(clear all TLV)
        if (amount.toLong() > 0 || getTransactionData().transType == Constants.TransType.REFUND) {
            checkCard()
        } else throw IllegalArgumentException("Amount must be above zero.")
    } catch (e: Exception) {
        e.printStackTrace()
    }

    fun tryAgain() = checkCard()

    fun getPin(dataCard: DataCard) {
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
        val len = posInstance().mEMVOptV2?.importOnlineProcStatus(tlvResponse.status, tags.toTypedArray(), values.toTypedArray(), out)
        PosLogger.e(PosLib.TAG, "finishOnlineProcessStatus::card update status code::: $len")
        len?.also {
            if (it < 0) {//Validar si esto aplica para MTIP 2.60 Refund
                if ((it == PosResult.SyncOperation.code || it == PosResult.TransRefused.code) && tlvResponse.status == 0) {
                    onFailureTrx(PosResult.SyncOperation)
                }
            }
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
        dataCard.cardNo = mapTags[Constants.TagsEmv.ENC_PAN.tag]?.value ?: ""
        dataCard.track1 = mapTags[Constants.TagsEmv.ENC_TRACK_1.tag]?.value ?: ""
        dataCard.track2 = mapTags[Constants.TagsEmv.ENC_TRACK_2.tag]?.value ?: ""
        dataCard.track3 = mapTags[Constants.TagsEmv.ENC_TRACK_3.tag]?.value ?: ""
        dataCard.holderName = mapTags[Constants.TagsEmv.CARDHOLDER_NAME.tag]?.value ?: ""
        dataCard.serviceCode = mapTags[Constants.TagsEmv.SERVICE_CODE.tag]?.value ?: ""
        dataCard.tlvData = getHexEmvTags(mapTags)
        dataCard.pinBlock = ByteUtil.bytes2HexStr(hexStrPin)
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
            //PosLogger.e(PosLib.TAG, "info::$info")
            mCardType = AidlConstants.CardType.MAGNETIC
            try {
                val dataCard = getDataCard(info)
                if (EmvUtil.isChipCard(dataCard.serviceCode) && !allowFallback) { //Tarjeta por chip no fallback
                    onFailureTrx(PosResult.CardDenial)
                    cancelProcessEmv()
                } else if (pinMustBeForced() || EmvUtil.requiredNip(dataCard.serviceCode))
                    initPinPad(dataCard)
                else
                    GlobalScope.launch(Dispatchers.Main) { goOnlineProcess(dataCard) }
            } catch (e: Exception) {
                 PosLogger.e(PosLib.TAG, e.toString())
                GlobalScope.launch (Dispatchers.Main){ onFailureTrx(PosResult.ErrorCheckCard) }
            }
        }

        @Throws(RemoteException::class)
        override fun findICCard(atr: String) {
            PosLogger.e(PosLib.TAG, "findICCard atr::$atr")
            mCardType = AidlConstants.CardType.IC
            transactProcess()
        }

        @Throws(RemoteException::class)
        override fun findRFCard(uuid: String) {
            PosLogger.e(PosLib.TAG, "findRFCard uuid::$uuid")
            mCardType = AidlConstants.CardType.NFC
            transactProcess()
        }

        @Throws(RemoteException::class)
        override fun onError(code: Int, message: String) {
            PosLogger.e(PosLib.TAG, "onError::$code message::$message")
            GlobalScope.launch(Dispatchers.Main) { onFailureTrx(getPosResult(code, PosResult.ErrorCheckCard.message)) }
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
            PosLogger.e(PosLib.TAG, "onAppFinalSelect tag9F06Value::$appSelected")
            if (appSelected.isNotEmpty()) {
                val isVisa = appSelected.startsWith("A000000003")
                val isMaster = appSelected.startsWith("A000000004")
                val isUnion = appSelected.startsWith("A000000333")
                val mAppSelect = when {
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
            PosLogger.e(PosLib.TAG, "onConfirmCardNo::$cardNo")
            mCardNo = cardNo
            posInstance().mEMVOptV2?.importCardNoStatus(0)
        }

        @Throws(RemoteException::class)
        override fun onRequestShowPinPad(pinType: Int, remainTime: Int) {
            PosLogger.e(PosLib.TAG, "onRequestShowPinPad pinType::$pinType")
            mPinType = pinType
            isRequestPin = true
            initPinPad()
        }

        @Throws(RemoteException::class)
        override fun onRequestSignature() {
            PosLogger.e(PosLib.TAG, "onRequestSignature")
            isRequestSignature = true
            posInstance().mEMVOptV2?.importSignatureStatus(0)
        }

        @Throws(RemoteException::class)
        override fun onOnlineProc() {
            val mapTags = tlvData
            emvTags = tagsTlvToTagsString(mapTags)
            GlobalScope.launch(Dispatchers.Main) { goOnlineProcess(getDataCard(mapTags)) }
        }

        override fun onCertVerify(p0: Int, p1: String?) {
            PosLogger.e(PosLib.TAG, "onCertVerify p0-> $p0 p1-> $p1")
        }

        @Throws(RemoteException::class)
        override fun onCardDataExchangeComplete() {
            PosLogger.e(PosLib.TAG, "onCardDataExchangeComplete")
        }

        @Throws(RemoteException::class)
        override fun onTransResult(code: Int, desc: String?) { //when has finalized process online
            PosLogger.e(PosLib.TAG, "onTransResult code: $code desc: $desc")
            if (getTransactionData().transType == Constants.TransType.REFUND && code == PosResult.TransTerminate.code) {//transResult does not matter when transaction is a refund
                onOnlineProc()
            } else if (code == PosResult.CardAbsentAproved.code)
                checkAndRemoveCard()
            else
                GlobalScope.launch(Dispatchers.Main) { onFailureTrx(getPosResult(code, customMessage ?: desc)) }
        }

        @Throws(RemoteException::class)
        override fun onConfirmationCodeVerified() { //Only confirmation phone required
            val outData = ByteArray(512)
            posInstance().mEMVOptV2?.getTlv(AidlConstants.EMV.TLVOpCode.OP_PAYPASS, "DF8129", outData)?.apply {
                if (this > 0) {
                    val data = ByteArray(this)
                    System.arraycopy(outData, 0, data, 0, this)
                    PosLogger.e(PosLib.TAG, "onRequestDataExchange DF8129:: ${ByteUtil.bytes2HexStr(data)}")
                }
            }
            posInstance().mReadCardOptV2?.cardOff(mCardType.value) // card off
            onFailureTrx(PosResult.SeePhone)
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
                        PosLogger.e(PosLib.TAG, "getCardNo error, code: $len"); return ""
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
        posInstance().mEMVOptV2?.setTlvList(AidlConstants.EMV.TLVOpCode.OP_PAYPASS, tagsPayPass, valuesPayPass)
    } catch (e: RemoteException) {
        PosLogger.e(PosLib.TAG, e.message)
    }

    fun cancelOperation() = try {
        cancelProcessEmv()
        onFailureTrx(PosResult.OperationCanceled)
    } catch (exe: Exception) {
        onFailureTrx(PosResult.OperationCanceled)
    }

    private fun getHexEmvTags(mapTags: Map<String, TLV?>) = StringBuilder().apply {
        for (tag in mapTags.keys)
            append(mapTags[tag]?.recoverToHexStr())
    }.toString()

    private fun clearVars() {
        mCardNo = ""
        allowFallback = false
        isRequestSignature = false
        hexStrPin = ByteArray(0)
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
            posInstance().mEMVOptV2?.getTlvList(AidlConstants.EMV.TLVOpCode.OP_NORMAL, tagList, outData)?.let {
                if (it > 0) {
                    val bytes = outData.copyOf(it)
                    val hexStr = ByteUtil.bytes2HexStr(bytes)
                    val tlvMap = TLVUtil.buildTLVMap(hexStr)
                    map.putAll(tlvMap)
                }
            }
            //Contacless tags
            posInstance().mEMVOptV2?.getTlvList(AidlConstants.EMV.TLVOpCode.OP_PAYPASS, EmvUtil.payPassTags, outData)?.let {
                if (it > 0) {
                    val bytes = outData.copyOf(it)
                    val hexStr = ByteUtil.bytes2HexStr(bytes)
                    val tlvMap = TLVUtil.buildTLVMap(hexStr)
                    tlvMap["9F6E"]?.apply { map["9F6E"] = this }
                }
            }
            cleanMap(map)
        } catch (exe: Exception) {
            PosLogger.e(PosLib.TAG, "tlvData: " + exe.message)
            cancelOperation()
            emptyMap()
        }

    private fun cleanMap(mapTags: MutableMap<String, TLV>) = mapTags.let {//remove empty tags
        for ((key, tlv) in HashMap(it)) {
            if (tlv.value.isEmpty())
                it.remove(key)
        }
        it
    }

    private fun initPinPad(dataCard: DataCard? = null) {//dataCard must be different null for a manual pin
        try {
            val panBytes = mCardNo.substring(mCardNo.length - 13, mCardNo.length - 1).toByteArray(charset("US-ASCII"))
            val pinPadConfig = PinPadConfigV2()
            pinPadConfig.pinPadType = 1
            pinPadConfig.pinType = mPinType
            pinPadConfig.isOrderNumKey = true
            pinPadConfig.pan = panBytes
            pinPadConfig.timeout = 60 * 1000 // input password timeout
            pinPadConfig.pinKeyIndex = 11 // pik index
            pinPadConfig.maxInput = 12
            pinPadConfig.minInput = 4
            pinPadConfig.keySystem = 0
            pinPadConfig.algorithmType = 0

            onShowPinPad(object : PinPadListenerV2.Stub() {
                override fun onPinLength(len: Int) {
                    //Length pin
                }

                override fun onConfirm(i: Int, pinBlock: ByteArray?) {
                    try {
                        pinBlock?.apply {
                            hexStrPin = pinBlock
                            dataCard?.apply {
                                this.pinBlock = ByteUtil.bytes2HexStr(hexStrPin)
                                GlobalScope.launch(Dispatchers.Main) { goOnlineProcess(this@apply) }
                            } ?: posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 0)
                        } ?: posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 2)

                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, "initPinPad::onConfirm" + exe.message)
                        cancelOperation()
                    }
                }

                override fun onCancel() {
                    try {
                        posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 1)
                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, "initPinPad::onCancel" + exe.message)
                        cancelOperation()
                    }
                }

                override fun onError(code: Int) {
                    try {
                        PosLogger.e(PosLib.TAG, "onError Pin:$code")
                        posInstance().mEMVOptV2?.importPinInputStatus(mPinType, 3)
                    } catch (exe: RemoteException) {
                        PosLogger.e(PosLib.TAG, "initPinPad::onError" + exe.message)
                        cancelOperation()
                    }
                }
            }, pinPadConfig)
        } catch (exe: Exception) {
            PosLogger.e(PosLib.TAG, "initPinPad::" + exe.message)
            cancelOperation()
        }
    }

    private fun checkAndRemoveCard() {
        try {//Check and notify remove card
            val status = posInstance().mReadCardOptV2?.getCardExistStatus(mCardType.value)?.also {
                if (it < 0) { onFailureTrx(PosResult.ErrorCheckPresentCard); return }
            }
            when (status) {
                AidlConstants.CardExistStatus.CARD_ABSENT -> { onApprovedTrx() }
                AidlConstants.CardExistStatus.CARD_PRESENT -> {
                    onFailureTrx(PosResult.CardPresentWait)
                    posInstance().mBasicOptV2?.buzzerOnDevice(1, 2750, 200, 0)
                    loopRemoveCard()
                }
                else -> throw IllegalArgumentException("Unknown result checkAndRemoveCard.")
            }
        } catch (e: Exception) {
            PosLogger.e(PosLib.TAG, e.message)
        }
    }

    private fun loopRemoveCard() = GlobalScope.launch(Dispatchers.Main) {
        delay(1000)
        checkAndRemoveCard()
    }

    protected abstract fun goOnlineProcess(dataCard: DataCard)

    abstract fun onFailureTrx(result: PosResult)

    abstract fun onApprovedTrx()

    abstract fun getCheckCardType(): Int

    abstract fun pinMustBeForced(): Boolean

    abstract fun onShowPinPad(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2)

    abstract fun onSelectEmvApp(listEMVApps: List<String>, applicationEmv: AppEmvSelectListener)

    abstract fun getTransactionData(): TransactionData
}