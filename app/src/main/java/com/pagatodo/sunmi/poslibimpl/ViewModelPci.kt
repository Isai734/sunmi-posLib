package com.pagatodo.sunmi.poslibimpl

import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.viewmodel.SunmiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ViewModelPci : SunmiViewModel<String>() {

    fun purchase() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(2500L)
            pciViewModel.value = Results.Success("Venta Exitosa!")
        }
    }

    fun sync() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(2500L)
            syncViewModel.value = Results.Success("Sync Exitosa!")
        }
    }

}