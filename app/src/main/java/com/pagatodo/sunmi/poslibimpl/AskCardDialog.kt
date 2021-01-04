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

class AskCardDialog : DialogFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //(context as AbstractActivity<*>).setOnFullView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_ask_for_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}