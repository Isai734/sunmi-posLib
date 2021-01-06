package com.pagatodo.sunmi.poslibimpl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import java.lang.Exception

class ViewModelPci : ViewModel() {
    val purchaseMlData = MutableLiveData<Results<RespuestaTrxCierreTurno>>()

    fun purchase() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(2500L)
            //purchaseMlData.value =
        }
    }
}