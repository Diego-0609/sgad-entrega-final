package com.sgad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String CLAVE = "una-clave-de-prueba-con-mas-de-32-caracteres-ok";

    @Test
    void generaYValidaUnTokenConUsuarioYRol() {
        JwtService jwt = new JwtService(CLAVE, 3600);
        String token = jwt.generar("admin", "ROLE_ADMIN");

        Claims claims = jwt.validar(token);
        assertEquals("admin", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("rol", String.class));
        assertEquals("sgad-backend", claims.getIssuer());
        assertEquals(3, token.split("\\.").length, "un JWT tiene 3 partes: cabecera.contenido.firma");
    }

    @Test
    void permiteUnaVidaPropiaPorToken() {
        JwtService jwt = new JwtService(CLAVE, 3600);
        Claims claims = jwt.validar(jwt.generar("admin", "ROLE_ADMIN", 900));
        assertEquals(900, (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000);
    }

    @Test
    void rechazaUnTokenVencido() {
        JwtService jwt = new JwtService(CLAVE, -10);   // ya nace vencido
        String token = jwt.generar("admin", "ROLE_ADMIN");
        assertThrows(ExpiredJwtException.class, () -> jwt.validar(token));
    }

    @Test
    void rechazaUnTokenAlteradoPorElUsuario() {
        JwtService jwt = new JwtService(CLAVE, 3600);
        String[] partes = jwt.generar("deportista", "ROLE_DEPORTISTA").split("\\.");
        // El atacante cambia el contenido para ponerse rol de admin, pero conserva la firma vieja
        String contenidoFalso = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"sgad-backend\",\"sub\":\"deportista\",\"rol\":\"ROLE_ADMIN\"}".getBytes());
        String falso = partes[0] + "." + contenidoFalso + "." + partes[2];
        assertThrows(JwtException.class, () -> jwt.validar(falso));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String ajeno = new JwtService("otra-clave-distinta-con-mas-de-32-caracteres!!", 3600).generar("admin", "ROLE_ADMIN");
        assertThrows(JwtException.class, () -> new JwtService(CLAVE, 3600).validar(ajeno));
    }

    @Test
    void rechazaTokenSinFirmaAlgNone() {
        String cab = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String cont = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"sgad-backend\",\"sub\":\"admin\",\"rol\":\"ROLE_ADMIN\"}".getBytes());
        assertThrows(JwtException.class, () -> new JwtService(CLAVE, 3600).validar(cab + "." + cont + "."));
    }

    @Test
    void rechazaUnTokenSinVencimiento() {
        String sinExp = io.jsonwebtoken.Jwts.builder().issuer("sgad-backend").subject("admin")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(CLAVE.getBytes())).compact();
        assertThrows(JwtException.class, () -> new JwtService(CLAVE, 3600).validar(sinExp));
    }

    @Test
    void exigeUnaClaveLarga() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta", 3600));
    }
}
