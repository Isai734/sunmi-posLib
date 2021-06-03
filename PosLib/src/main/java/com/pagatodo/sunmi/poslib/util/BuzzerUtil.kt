package com.pagatodo.sunmi.poslib.util

import com.pagatodo.sunmi.poslib.posInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object BuzzerUtil {
    fun doBeep(result: PosResult){
        when(result){
            PosResult.SeePhone -> seePhone()
            else -> return
        }
    }
    private fun seePhone(){
        GlobalScope.launch (Dispatchers.IO){
            posInstance().mBasicOptV2?.buzzerOnDevice(2, 750, 200, 200)
        }
    }
}