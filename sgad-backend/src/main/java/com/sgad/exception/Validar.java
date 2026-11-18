package com.sgad.exception;

/** Pequeñas validaciones reutilizadas por los servicios. */
public final class Validar {

    private Validar() {}

    /** Exige que un id (referencia a otra colección) venga informado. */
    public static void idObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new SolicitudInvalidaException("El campo '" + campo + "' es obligatorio.");
        }
    }

    public static boolean informado(String valor) {
        return valor != null && !valor.isBlank();
    }
}
