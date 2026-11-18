package com.sgad.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgad.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Qué responde el sistema cuando la seguridad dice "no":
 *
 *  - 401 no autenticado        -> /api/**: JSON {codigo: NO_AUTENTICADO}
 *  - 403 sin permisos del rol  -> /api/**: JSON {codigo: ACCESO_DENEGADO} | web: página /403
 *  - sesión vencida (timeout)  -> /api/**: JSON {codigo: SESION_EXPIRADA}  | web: /login?expired=true
 *  - sesión cerrada por haber iniciado sesión en otro lugar
 *                              -> /api/**: JSON {codigo: SESION_REEMPLAZADA} | web: /login?concurrent=true
 *
 * Así Angular (que consume /api) recibe JSON claro y las páginas del
 * backend (Thymeleaf) reciben redirecciones, cada uno en su formato.
 */
@Component
@RequiredArgsConstructor
public class SeguridadHandlers implements AuthenticationEntryPoint, AccessDeniedHandler,
        InvalidSessionStrategy, SessionInformationExpiredStrategy {

    private final ObjectMapper objectMapper;
    private final CookieClearingLogoutHandler borrarCookie = new CookieClearingLogoutHandler("JSESSIONID");

    /** Usuario sin sesión intentando entrar a /api/**. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribirJson(request, response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                "Debes iniciar sesión para continuar.");
    }

    /** Usuario autenticado pero sin el rol necesario. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        if (esApi(request)) {
            escribirJson(request, response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                    "No tienes permisos para realizar esta acción.");
        } else {
            response.sendRedirect(request.getContextPath() + "/403");
        }
    }

    /** La cookie de sesión ya no corresponde a una sesión válida (expiró por inactividad). */
    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Se borra la cookie vencida para no entrar en un bucle de redirecciones.
        borrarCookie.logout(request, response, null);
        if (esApi(request)) {
            escribirJson(request, response, HttpStatus.UNAUTHORIZED, "SESION_EXPIRADA",
                    "Tu sesión expiró por inactividad. Inicia sesión de nuevo.");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?expired=true");
        }
    }

    /** La sesión fue cerrada porque el mismo usuario inició otra (límite de sesiones simultáneas). */
    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException {
        HttpServletRequest request = event.getRequest();
        HttpServletResponse response = event.getResponse();
        if (esApi(request)) {
            escribirJson(request, response, HttpStatus.UNAUTHORIZED, "SESION_REEMPLAZADA",
                    "Tu sesión se cerró porque iniciaste sesión en otro lugar.");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?concurrent=true");
        }
    }

    private boolean esApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    public void escribirJson(HttpServletRequest request, HttpServletResponse response,
                              HttpStatus status, String codigo, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiError.de(status, codigo, mensaje, request.getRequestURI(), null));
    }
}
