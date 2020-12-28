package com.pagatodo.sunmi.poslib.model

import android.util.Log

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Results<out T : Any> {
    companion object {
        const val TAG = "PosException"
    }

    data class Success<T : Any>(val data: T) : Results<T>()
    data class Failure(val exception: Exception) : Results<Nothing>() {
        init {
            Log.e(TAG, exception.message, exception)
        }
    }

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$exception]"
        }
    }
}
