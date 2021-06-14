package com.pagatodo.sunmi.poslib.viewmodel

import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.repository.RepositoryEmv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.oper.TipoOperacion

open class EmvViewModel : AbstractViewModel<AbstractRespuesta>() {

    fun executeEmvOpr(operation: TipoOperacion, product: String, fields: List<String>, dataOpTarjeta: DataOpTarjeta) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                RepositoryEmv.execOperationEmv(operation, product, fields, dataOpTarjeta) { pciViewModel.value = it }
            } catch (e: Exception) {
                pciViewModel.value = Results.Failure(e)
            }
        }
    }

    fun executeSync(operation: TipoOperacion, product: String, fields: List<String>, dataOpTarjeta: DataOpTarjeta, stan: Long) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                RepositoryEmv.execOperationEmv(operation, product, fields, dataOpTarjeta, stan) { syncViewModel.value = it }
            } catch (e: Exception) {
                syncViewModel.value = Results.Failure(e)
            }
        }
    }
}