package com.sgad.controller;

import com.sgad.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.sgad.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Donde se obtiene un JWT. Hay dos caminos:
 *
 *  POST /api/auth/token   {username, password}   -> para clientes de la API (Postman, apps, scripts).
 *                          Valida contra MongoDB (BCrypt) y responde el token. No crea sesión.
 *  GET  /api/auth/token   -> para quien YA inició sesión en la web (formulario o Google):
 *                          cambia esa sesión por un token.
 *
 * Luego el cliente manda el token en cada petición:   Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtService jwtService;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest peticion) throws Exception {
        AuthenticationManager manager = authenticationConfiguration.getAuthenticationManager();
        try {
            Authentication auth = manager.authenticate(
                    new UsernamePasswordAuthenticationToken(peticion.username(), peticion.password()));
            return ResponseEntity.ok(respuesta(auth, jwtService.getSegundosDeVida()));
        } catch (AuthenticationException e) {
            // Mismo mensaje para "no existe" y "clave mala": no se revela qué usuarios existen.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.de(HttpStatus.UNAUTHORIZED,
                    "CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos.", "/api/auth/token", null));
        }
    }

    /**
     * El token que se cambia por una sesión dura lo MISMO que la inactividad permitida de esa sesión
     * (15 min por defecto). Angular lo renueva con la cookie mientras el usuario esté activo; si la
     * sesión murió por inactividad, la renovación falla y se vuelve al login.
     */
    @GetMapping("/token")
    public Map<String, Object> deSesion(Authentication authentication, HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        long segundos = sesion != null && sesion.getMaxInactiveInterval() > 0
                ? sesion.getMaxInactiveInterval() : jwtService.getSegundosDeVida();
        return respuesta(authentication, segundos);
    }

    private Map<String, Object> respuesta(Authentication auth, long segundos) {
        String rol = auth.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("");
        return Map.of(
                "accessToken", jwtService.generar(auth.getName(), rol, segundos),
                "tokenType", "Bearer",
                "expiresIn", segundos,
                "username", auth.getName(),
                "rol", rol
        );
    }
}
