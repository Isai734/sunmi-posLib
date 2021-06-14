package com.pagatodo.sunmi.poslib.model.repository

import androidx.lifecycle.MutableLiveData
import com.pagatodo.sigmalib.transacciones.AbstractTransaccion
import com.pagatodo.sigmalib.transacciones.TransaccionFactory
import com.pagatodo.sunmi.poslib.model.Results
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.oper.TipoOperacion

object RepositoryEmv {

    fun execOperationEmv(operation: TipoOperacion, product: String, fields: List<String>, dataOpTarjeta: DataOpTarjeta, stan: Long = 0L, result: (Results<AbstractRespuesta>) -> Unit) {
        TransaccionFactory.crearTransacion<AbstractTransaccion>(operation, { response ->
            if (response.isCorrecta || response.operacionSiguiente.mtiNext != null)
                result(Results.Success(response))
            else
                result(Results.Failure(Exception(response.msjError)))
        }, { error -> result(Results.Failure(Exception(error))) }).withProcod(product).withFields(fields).withStan(stan).withDatosOpTarjeta(dataOpTarjeta).realizarOperacion()
    }
}
