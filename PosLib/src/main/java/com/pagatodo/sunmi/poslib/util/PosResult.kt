package com.pagatodo.sunmi.poslib.util

import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.requireContext

enum class PosResult(var code: Int, var tile:String, var message: String? = null ) {
    CardDenial(-33, getMessage(R.string.card_denial)),
    ErrorRepeatCall(-20001, getMessage(R.string.repeat_call)),
    NfcTerminated(-2520, getMessage(R.string.error_card_no_supported)),
    FallBack(-2800, getMessage(R.string.chip_fallback)),
    TransRefused(-4000, getMessage(R.string.trans_refused)),
    OtherInterface(-4001, getMessage(R.string.other_interface)),
    TransTerminate(-4002, getMessage(R.string.card_no_supported_msg), getMessage(R.string.card_no_supported_title)),
    SeePhone(-4003, getMessage(R.string.see_phone)),
    BlockedApp(-4105, getMessage(R.string.blocked_app)),
    NoCommonAppNfc(-4106, getMessage(R.string.common_app_nfc)),
    FallBackCommonApp(-4107, getMessage(R.string.chip_fallback)),
    DataCardWithError(-4108, getMessage(R.string.common_app_nfc)),
    DoSyncOperation(-4115, getMessage(R.string.sync_operation)),
    NoMagneticMode(-4122, getMessage(R.string.magnetic_mode_not_support)),
    CardNoSupported(-4125, getMessage(R.string.error_card_no_supported)),
    OnlineError(-50024,  getMessage(R.string.error_online_process_title)),
    FinalSelectApp(-50026, getMessage(R.string.error_card_no_supported)),
    PinTimeOut(-60001, getMessage(R.string.error_time_out)),

    OnlineApproved(0, getMessage(R.string.transaction_approved)),
    OfflineDecline(2, getMessage(R.string.offline_declined)),
    ReplaceCard(4, getMessage(R.string.replace_card)),
    ErrorCheckCard(5, getMessage(R.string.check_card)),
    ErrorSelectApp(6, getMessage(R.string.select_app)),
    Track2Error(7, getMessage(R.string.track2_error)),
    OperationCanceled(8, getMessage(R.string.cancel_operation)),
    ErrorCheckPresentCard(9, getMessage(R.string.error_check_present_card)),
    CardPresentWait(10, getMessage(R.string.card_present_wait)),
    SyncOperationFailed(11, getMessage(R.string.sync_operation_failed)),
    SyncOperationSuccess(12, getMessage(R.string.sync_operation_success)),
    NextOperation(13, getMessage(R.string.sync_operation)),
    ErrorEmptyPin(14, getMessage(R.string.empty_pin)),
    NoSecretWrong(15, getMessage(R.string.secret_wrong)),
    InfoPinOk(16, getMessage(R.string.pin_ok)),
    ErrorEmptySing(17, getMessage(R.string.empty_sing)),
    Generic(100, getMessage(R.string.generic));
}

fun getPosResult(code: Int, message: String?): PosResult {
    for (pr in PosResult.values()){
        if (pr == PosResult.OnlineError && pr.code == code)
            return pr.apply { this.tile = message ?: ""  }
        else if (pr != PosResult.Generic && pr.code == code)
            return pr
    }

    return PosResult.Generic.apply {
        this.code = code
        this.tile = message ?: ""
    }
}

private fun getMessage(resourceId: Int) = requireContext().getString(resourceId)