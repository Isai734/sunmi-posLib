package com.pagatodo.sunmi.poslib.util

class EmvException internal constructor(private val error: ValidationError) : Throwable() {
    enum class ValidationError {
        ERROR_EXPIRY_DATE, ERROR_FALLBACK, ERROR_CASHBACK_PERFIL, ERROR_CASHBACK_PAIS, ERROR_BINES, ERROR_PAN, ERROR_CUOTAS
    }

    override val message: String?
        get() {
            when (error) {
                ValidationError.ERROR_FALLBACK -> {
                    return "Fallback No soportado en esta Transacción "
                }
                ValidationError.ERROR_EXPIRY_DATE -> {
                    return "Tarjeta Vencida"
                }
                ValidationError.ERROR_CASHBACK_PERFIL -> {
                    return "CashBack No Soportado por el Perfil del Usuario "
                }
                ValidationError.ERROR_CASHBACK_PAIS -> {
                    return "CashBack No Soportado, La Tarjeta no Permite Retiros en este País"
                }
                ValidationError.ERROR_BINES -> {
                    return "Tarjeta Inválida"
                }
                ValidationError.ERROR_CUOTAS -> {
                    return "Cuota Inválida"
                }
                ValidationError.ERROR_PAN -> {
                    return "Ultimos 4 Dígitos Erróneos"
                }
                else -> return super.message
            }
        }
}