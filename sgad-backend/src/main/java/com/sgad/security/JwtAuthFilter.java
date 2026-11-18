package com.sgad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro del JWT: en cada petición con "Authorization: Bearer <token>":
 *   1. valida el token (firma, emisor, vencimiento),
 *   2. vuelve a leer el usuario en MongoDB (si lo deshabilitaron o lo borraron, el token deja de servir
 *      y el rol vigente es el de la base de datos, no el que traía el token),
 *   3. deja al usuario autenticado para esa petición (sin crear sesión ni cookie).
 *
 * NO es un @Component a propósito: solo se usa dentro de la cadena JWT de SecurityConfig.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SeguridadHandlers respuestas;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                         SeguridadHandlers respuestas) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.respuestas = respuestas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        String token = cabecera.substring(7).trim();

        try {
            Claims claims = jwtService.validar(token);
            UserDetails usuario = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!usuario.isEnabled()) {
                throw new DisabledException("Usuario deshabilitado");
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new JwtAuthenticationToken(usuario, claims.getExpiration().toInstant(), usuario.getAuthorities()));
        } catch (ExpiredJwtException e) {
            respuestas.escribirJson(request, response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRADO",
                    "Tu token venció. Solicita uno nuevo.");
            return;
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException | DisabledException e) {
            respuestas.escribirJson(request, response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO",
                    "El token no es válido.");
            return;
        }
        chain.doFilter(request, response);
    }
}
