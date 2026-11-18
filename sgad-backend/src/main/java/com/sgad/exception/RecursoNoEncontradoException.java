package com.sgad.exception;

/** El recurso pedido (escuela, deportista, etc.) no existe. Se responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
