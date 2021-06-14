package com.pagatodo.sunmi.poslib.util;

public class EmvException extends Throwable {

    public enum VALIDATION_ERROR {
        ERROR_EXPIRY_DATE,
        ERROR_FALLBACK,
        ERROR_CASHBACK_PERFIL,
        ERROR_CASHBACK_PAIS,
        ERROR_BINES,
        ERROR_PAN,
        ERROR_CUOTAS
    }

    private final VALIDATION_ERROR error;

    EmvException(final VALIDATION_ERROR validationError) {
        this.error = validationError;
    }

    @Override
    public String getMessage() {
        if (error == VALIDATION_ERROR.ERROR_FALLBACK) {
            return "Fallback No soportado en esta Transacción ";
        } else if (error == VALIDATION_ERROR.ERROR_EXPIRY_DATE) {
            return "Tarjeta Vencida";
        } else if (error == VALIDATION_ERROR.ERROR_CASHBACK_PERFIL) {
            return "CashBack No Soportado por el Perfil del Usuario ";
        } else if (error == VALIDATION_ERROR.ERROR_CASHBACK_PAIS) {
            return "CashBack No Soportado, La Tarjeta no Permite Retiros en este País";
        } else if (error == VALIDATION_ERROR.ERROR_BINES) {
            return "Tarjeta Inválida";
        } else if (error == VALIDATION_ERROR.ERROR_CUOTAS) {
            return "Cuota Inválida";
        } else if (error == VALIDATION_ERROR.ERROR_PAN) {
            return "Ultimos 4 Dígitos Erróneos";
        }
        return super.getMessage();
    }
}