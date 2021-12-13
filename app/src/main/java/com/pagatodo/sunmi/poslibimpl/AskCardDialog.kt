package com.pagatodo.sunmi.poslibimpl

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.pagatodo.sunmi.poslib.databinding.RequestCardDialogBinding
import com.pagatodo.sunmi.poslibimpl.databinding.ActivityMainBinding
import com.pagatodo.sunmi.poslibimpl.databinding.DialogAskForCardBinding
import java.util.*
import kotlin.collections.ArrayList

fun main(){
    val list = listOf("Isai", "Castro", "Alvarado").iterator()
    load(list)
}

fun load(lis: Iterator<Any>){
    if(!lis.hasNext()) return
    println(lis.next().toString())
    load(lis)
}

class AskCardDialog : DialogFragment() {

    private lateinit var binding: DialogAskForCardBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        binding = DialogAskForCardBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.requestCardAnim.playAnimation()
    }
}

