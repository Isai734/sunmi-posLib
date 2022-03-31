package com.pagatodo.sunmi.poslib.util

import net.fullcarga.android.api.data.StanProvider

abstract class StanProviderNext: StanProvider {

    var isLogin = true

    abstract fun createNext():Long

}