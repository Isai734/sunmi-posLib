package com.pagatodo.sunmi.poslib.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pagatodo.sunmi.poslib.model.Results

open class SunmiViewModel<E : Any> : ViewModel() {
    val pciViewModel = MutableLiveData<Results<E>>()
    val syncViewModel = MutableLiveData<Results<E>>()
}