package com.pagatodo.sunmi.poslibimpl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pagatodo.sunmi.poslib.model.Results
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class ViewModelPci : ViewModel() {
    val purchaseMlData = MutableLiveData<Results<String>>()

    fun purchase() {
        viewModelScope.launch(Dispatchers.Main) {
            delay(1500L)
            purchaseMlData.value = Results.Failure(Exception("Venta Fallida!"))
        }
    }
}