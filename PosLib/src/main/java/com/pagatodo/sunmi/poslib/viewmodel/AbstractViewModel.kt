package com.pagatodo.sunmi.poslib.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import com.pagatodo.sunmi.poslib.model.repository.RepositoryEmv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.DataOpTarjeta
import net.fullcarga.android.api.data.respuesta.AbstractRespuesta
import net.fullcarga.android.api.oper.TipoOperacion

open class AbstractViewModel<E : Any> : ViewModel() {
    val pciViewModel = MutableLiveData<Results<E>>()
    val syncViewModel = MutableLiveData<Results<E>>()
}