package com.sgad.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unifica el formato de los errores que NO pasan por un controlador (ruta
 * inexistente, método no permitido, errores del contenedor...). Son los que
 * Spring resuelve en /error. Así TODA respuesta de error JSON de la API
 * tiene la misma forma (ver ApiError) y Angular siempre lee "mensaje".
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> base = super.getErrorAttributes(webRequest, options);
        int codigoHttp = base.get("status") instanceof Integer i ? i : 500;
        HttpStatus status = HttpStatus.resolve(codigoHttp);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("codigo", status.name());
        cuerpo.put("mensaje", mensaje(status));
        cuerpo.put("ruta", base.get("path"));
        return cuerpo;
    }

    private static String mensaje(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para este recurso.";
            case FORBIDDEN -> "No tienes permisos para realizar esta acción.";
            case UNAUTHORIZED -> "Debes iniciar sesión para continuar.";
            default -> status.is4xxClientError() ? "La petición no es válida."
                                                 : "Ocurrió un error inesperado en el servidor.";
        };
    }
}
