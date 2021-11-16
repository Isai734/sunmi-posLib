package com.pagatodo.sunmi.poslibimpl

import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.viewmodel.AbstractViewModel
import com.pagatodo.sunmi.poslib.viewmodel.EmvViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class ViewModelPci : AbstractViewModel<String>() {

    fun purchase() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(1500L)
            pciViewModel.value = Results.Success("Venta precesada correctamente!")
        }
    }

    fun sync() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(1500L)
            syncViewModel.value = Results.Success("Sync Exitosa!")
        }
    }

}