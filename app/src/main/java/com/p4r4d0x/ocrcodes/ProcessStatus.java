package com.p4r4d0x.ocrcodes;

/**
 * Enumerado que recoge los estados del procesamiento
 * Created by PCCom on 07/01/2018.
 */

public enum ProcessStatus {
    /**
     * La imagen esta borrosa
     */
    Blurry,
    /**
     * La imagen tiene demasiada luz
     */
    HihghLight,
    /**
     * Valor inválido extraido
     */
    Invalid,
    /**
     * Valor extraido válido
     */
    Valid,
    /**
     * No se ajusta al patrón del código
     */
    WronmPattern
}
