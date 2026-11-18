package com.sgad.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.sgad.security.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint usado por el frontend Angular para saber si hay una sesión
 * autenticada activa, con qué rol y cuánto dura la sesión por inactividad
 * (para mostrar la cuenta regresiva y avisar antes de que expire).
 * Si no hay sesión, Spring Security responde 401 en JSON (ver
 * SeguridadHandlers) y Angular redirige al login del backend.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication, HttpServletRequest request) {
        String rol = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        HttpSession sesion = request.getSession(false);
        int segundos = sesion != null ? sesion.getMaxInactiveInterval() : 0;

        // Con qué se autenticó la petición: cookie de sesión, token JWT o Google (sesión creada vía OAuth2)
        String metodo = authentication instanceof JwtAuthenticationToken ? "JWT"
                : authentication instanceof OAuth2AuthenticationToken ? "GOOGLE" : "SESION";

        Map<String, Object> respuesta = new java.util.LinkedHashMap<>();
        respuesta.put("username", authentication.getName());
        respuesta.put("rol", rol);
        respuesta.put("sessionTimeoutSeconds", segundos);
        respuesta.put("metodo", metodo);
        if (authentication instanceof JwtAuthenticationToken jwt) {
            respuesta.put("tokenExpiraEn", jwt.getExpiraEn().toString());
        }
        return respuesta;
    }
}
