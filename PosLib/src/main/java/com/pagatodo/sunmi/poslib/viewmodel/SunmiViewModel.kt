package com.pagatodo.sunmi.poslib.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pagatodo.sunmi.poslib.model.Results

open class SunmiViewModel : ViewModel() {
    val pciViewModel = MutableLiveData<Results<String>>()
    val syncViewModel = MutableLiveData<Results<String>>()
}