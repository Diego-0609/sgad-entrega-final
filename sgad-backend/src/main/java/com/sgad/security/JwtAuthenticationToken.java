package com.sgad.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;

/** Autenticación creada a partir de un JWT válido (distinta de la de sesión/cookie). */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails usuario;
    private final Instant expiraEn;

    public JwtAuthenticationToken(UserDetails usuario, Instant expiraEn,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.usuario = usuario;
        this.expiraEn = expiraEn;
        setAuthenticated(true);
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return usuario;
    }

    @Override
    public String getName() {
        return usuario.getUsername();
    }
}
