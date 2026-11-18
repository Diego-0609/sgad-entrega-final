package com.sgad.exception;

/** La petición es incorrecta (dato faltante, referencia a un id que no existe, etc.). Se responde 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
