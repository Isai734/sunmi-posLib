package com.pagatodo.sunmi.poslib.util

import net.fullcarga.android.api.data.StanProvider

interface StanProviderNext: StanProvider {
    fun createNext(): Long
}