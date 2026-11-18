package com.sgad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JSON Web Token (JWT): crea y valida los tokens de acceso de la API.
 *
 * Un JWT son 3 partes separadas por puntos:  CABECERA . CONTENIDO . FIRMA
 *   - CONTENIDO (claims): quién es (sub = username), su rol, cuándo se creó (iat) y cuándo vence (exp).
 *   - FIRMA: se calcula con una clave secreta del servidor (HMAC-SHA). Si alguien cambia
 *     el contenido (por ejemplo su rol), la firma deja de coincidir y el token se rechaza.
 *
 * La clave NO está en el código: viene de la variable de entorno SGAD_JWT_SECRET.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    static final String EMISOR = "sgad-backend";
    static final String CLAVE_POR_DEFECTO = "sgad-clave-SOLO-para-desarrollo-cambiala-en-produccion-123456";

    private final SecretKey clave;
    private final long segundosDeVida;

    public JwtService(@Value("${sgad.jwt.secret}") String secreto,
                      @Value("${sgad.jwt.expiration-seconds:3600}") long segundosDeVida) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("sgad.jwt.secret (SGAD_JWT_SECRET) debe tener al menos 32 caracteres");
        }
        if (CLAVE_POR_DEFECTO.equals(secreto)) {
            log.warn("JWT: usando la clave de DESARROLLO. En un servidor define SGAD_JWT_SECRET con una clave propia.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.segundosDeVida = segundosDeVida;
    }

    public long getSegundosDeVida() {
        return segundosDeVida;
    }

    /** Crea un token firmado para el usuario con la vida por defecto (sgad.jwt.expiration-seconds). */
    public String generar(String username, String rol) {
        return generar(username, rol, segundosDeVida);
    }

    /** Igual, pero con una vida propia (se usa para dar tokens que duran lo mismo que la sesión). */
    public String generar(String username, String rol, long segundos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(username)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(segundos)))
                .signWith(clave)
                .compact();
    }

    /**
     * Verifica firma, emisor y vencimiento. Si algo falla lanza una JwtException
     * (ExpiredJwtException cuando solo está vencido, para poder avisar distinto).
     */
    public Claims validar(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // jjwt acepta tokens sin "exp"; aquí todo token DEBE vencer (si no, valdría para siempre).
        if (claims.getExpiration() == null || claims.getSubject() == null) {
            throw new MalformedJwtException("El token debe incluir sub y exp");
        }
        return claims;
    }
}
