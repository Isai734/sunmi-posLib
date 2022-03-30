package com.pagatodo.sunmi.poslib.viewmodel

import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.repository.RepositoryEmv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.oper.TipoOperacion

open class EmvViewModel : AbstractViewModel<AbstractRespuesta>() {

    fun executeEmvOpr(operation: TipoOperacion, product: String, fields: List<String>, dataOpTarjeta: DataOpTarjeta) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO){
                    RepositoryEmv.execOperationEmv(operation, product, fields, dataOpTarjeta) { pciViewModel.postValue(it) }
                }
            } catch (e: Exception) {
                pciViewModel.value = Results.Failure(e)
            }
        }
    }
}