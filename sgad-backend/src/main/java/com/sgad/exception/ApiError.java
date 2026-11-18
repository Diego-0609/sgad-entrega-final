package com.sgad.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Formato único de error que devuelve la API REST (/api/**), sea cual sea
 * la causa: validación, recurso inexistente, sesión vencida, permisos, etc.
 * Angular lee "mensaje" (y "errores" por campo) para mostrárselo al usuario.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String ruta,
        Map<String, String> errores
) {
    public static ApiError de(org.springframework.http.HttpStatus status, String codigo, String mensaje,
                              String ruta, Map<String, String> errores) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, ruta, errores);
    }
}
