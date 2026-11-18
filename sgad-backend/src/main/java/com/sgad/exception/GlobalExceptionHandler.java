package com.sgad.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo CENTRALIZADO de errores de la API REST.
 *
 * Ningún controlador necesita try/catch: cualquier excepción llega aquí y
 * se convierte en una respuesta JSON uniforme (ver ApiError) con el código
 * HTTP correcto. Los errores inesperados se registran en el log pero al
 * cliente solo se le muestra un mensaje genérico (nunca detalles internos).
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Errores propios de la aplicación ──────────────────────────────

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parámetro '" + ex.getName() + "' tiene un formato no válido.", req.getRequestURI(), null);
    }

    // ── Seguridad y base de datos ─────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tienes permisos para realizar esta acción.", req.getRequestURI(), null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> baseDeDatos(DataAccessException ex, HttpServletRequest req) {
        log.error("Error de acceso a MongoDB en {}", req.getRequestURI(), ex);
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "BASE_DE_DATOS",
                "No fue posible acceder a la base de datos. Intenta de nuevo en unos momentos.",
                req.getRequestURI(), null);
    }

    // ── Cualquier otro error inesperado ───────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Intenta de nuevo o contacta al administrador.",
                req.getRequestURI(), null);
    }

    // ── Errores estándar de Spring MVC (JSON mal formado, validación...) ──

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiError.de(HttpStatus.BAD_REQUEST, "VALIDACION",
                        "Hay datos incorrectos en el formulario.", ruta(request), errores));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiError.de(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                        "El cuerpo de la petición no es válido. Revisa los tipos de datos "
                                + "(fecha yyyy-MM-dd, hora HH:mm, números sin texto).", ruta(request), null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String mensaje = switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para este recurso.";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado (se espera application/json).";
            default -> status.is4xxClientError() ? "La petición no es válida."
                                                 : "Ocurrió un error inesperado.";
        };
        return ResponseEntity.status(statusCode).headers(headers == null ? new HttpHeaders() : headers).body(
                ApiError.de(status, status.name(), mensaje, ruta(request), null));
    }

    // ── utilidades ────────────────────────────────────────────────────

    private static ResponseEntity<ApiError> respuesta(HttpStatus status, String codigo, String mensaje,
                                                      String ruta, Map<String, String> errores) {
        return ResponseEntity.status(status).body(ApiError.de(status, codigo, mensaje, ruta, errores));
    }

    private static String ruta(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}
