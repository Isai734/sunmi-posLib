package com.pagatodo.sunmi.poslibimpl

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.posInstance
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.bean.PinPadDataV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.android.synthetic.main.dialog_pin_pad_custom.*
import kotlinx.android.synthetic.main.view_fix_password_keyboard.*

class PinPadDialog : DialogFragment() {
    private lateinit var pinPadListenerV2: PinPadListenerV2
    private var mWidth = 239
    private var mHeight = 130
    private var mInterval = 1
    private val mKeyboardCoordinate = intArrayOf(0, 661)
    private val mCancelWidth = 112
    private val mCancelHeight = 112
    private val mCancelCoordinate = intArrayOf(0, 48)
    private var textAccept = "Aceptar"
    private var textCancel = "Cerrar"
    private var passwordLength = 4
    private var customPinPadConfigV2: PinPadConfigV2? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //(context as AbstractActivity<*>).setOnFullView()
    }

    fun setPinPadListenerV2(pinPadListenerV2: PinPadListenerV2.Stub) {
        this.pinPadListenerV2 = pinPadListenerV2
    }

    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_pin_pad_custom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        passwordEditText.setNumberDigits(passwordLength)
        passwordEditText.repaintDigit()

        btnPinPadAccept.text = textAccept
        btnPinPadCancel.text = textCancel
        initView()
    }

    private fun initView() {
        val mIntent = arguments
        customPinPadConfigV2 = mIntent?.getSerializable("PinPadConfigV2") as PinPadConfigV2
        initPinPad()
    }

    override fun onDismiss(dialog: DialogInterface) {
        posInstance().screenMonopoly()
        super.onDismiss(dialog)
    }

    override fun onStart() {
        super.onStart()
        posInstance().screenFinancialModel()
    }

    override fun onDestroyView() {
        posInstance().screenMonopoly()
        super.onDestroyView()
    }

    private fun initPinPad() {
        try {
            Log.i(TAG, "initPinPad")
            val result = posInstance().mPinPadOptV2?.initPinPad(customPinPadConfigV2, object : PinPadListenerV2.Stub() {
                override fun onPinLength(i: Int) {
                    activity?.runOnUiThread { showPasswordView(i) }

                    pinPadListenerV2.onPinLength(i)
                }

                @Throws(RemoteException::class)
                override fun onConfirm(i: Int, bytes: ByteArray?) {
                    posInstance().screenMonopoly()
                    dismiss()
                    pinPadListenerV2.onConfirm(i, bytes)
                }

                @Throws(RemoteException::class)
                override fun onCancel() {
                    posInstance().screenMonopoly()
                    dismiss()
                    pinPadListenerV2.onCancel()
                }

                @Throws(RemoteException::class)
                override fun onError(i: Int) {
                    posInstance().screenMonopoly()
                    dismiss()
                    pinPadListenerV2.onError(i)
                }
            })
            Log.i(TAG, "result: $result")
            getKeyboardCoordinate(result)
            fixPasswordKeyboard.keepScreenOn = true
            fixPasswordKeyboard.setKeyBoard(result)
            fixPasswordKeyboard.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getKeyboardCoordinate(keyBoardText: String?) {
        fixPasswordKeyboard.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        fixPasswordKeyboard.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val textView = fixPasswordKeyboard.key0
                        textView.getLocationOnScreen(mKeyboardCoordinate)
                        mWidth = textView.width
                        mHeight = textView.height
                        mInterval = 1
                        importPinPadData(keyBoardText)
                    }
                }
        )
    }

    private fun importPinPadData(text: String?) {
        val pinPadData = PinPadDataV2()
        pinPadData.numX = mKeyboardCoordinate[0]
        pinPadData.numY = mKeyboardCoordinate[1]
        pinPadData.numW = mWidth
        pinPadData.numH = mHeight
        pinPadData.lineW = mInterval
        pinPadData.cancelX = mCancelCoordinate[0]
        pinPadData.cancelY = mCancelCoordinate[1]
        pinPadData.cancelW = mCancelWidth
        pinPadData.cancelH = mCancelHeight
        pinPadData.lineW = 0
        pinPadData.rows = 6
        pinPadData.clos = 3
        keyMap(text, pinPadData)
        try {
            Log.i(TAG, pinPadData.toString())
            posInstance().mPinPadOptV2?.importPinPadData(pinPadData)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun showPasswordView(len: Int) {
        val sb = StringBuilder()
        for (i in 0 until len) {
            sb.append("*")
        }
        if(len==7){
            passwordEditText.setNumberDigits(12)
            passwordEditText.repaintDigit()
        }
        passwordEditText.text = sb.toString()
    }

    fun setPasswordLength(length: Int) {
        passwordLength = length
    }


    fun setTextAccept(accept: String) {
        textAccept = accept
    }

    fun setTextCancel(cancel: String) {
        textCancel = cancel
    }

    private fun keyMap(str: String?, data: PinPadDataV2) {
        data.keyMap = ByteArray(64)
        var i = 0
        var j = 0
        while (i < 15) {
            if (i == 9 || i == 11) {
                data.keyMap[i] = 0x00
                j--
            } else if (i == 12) {
                data.keyMap[i] = 0x1B
                j--
            } else if (i == 13) {
                data.keyMap[i] = 0x0C
                j--
            } else if (i == 14) {
                data.keyMap[i] = 0x0D
                j--
            } else {
                data.keyMap[i] = str?.let { it[j].toByte() } ?: run { "".toByte() }
            }
            i++
            j++
        }
    }

    companion object {
        val TAG: String = PinPadDialog::class.java.simpleName
        fun createInstance(padConfigV2: PinPadConfigV2): PinPadDialog {
            val args = Bundle()
            val pinPadDialog = PinPadDialog()
            args.putSerializable("PinPadConfigV2", padConfigV2)
            pinPadDialog.arguments = args
            return pinPadDialog
        }
    }

}