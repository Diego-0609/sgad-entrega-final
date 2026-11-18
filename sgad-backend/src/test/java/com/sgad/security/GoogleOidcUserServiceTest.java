package com.sgad.security;

import com.sgad.entity.Usuario;
import com.sgad.repository.UsuarioRepository;
import com.sgad.service.PerfilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Prueba el puente Google -> MongoDB sin necesitar internet ni credenciales de Google. */
class GoogleOidcUserServiceTest {

    private UsuarioRepository repo;
    private PerfilService perfil;
    private GoogleOidcUserService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioRepository.class);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        perfil = mock(PerfilService.class);
        servicio = new GoogleOidcUserService(repo, new BCryptPasswordEncoder(), perfil);
    }

    private OidcUser googleDice(String email, Boolean verificado) {
        OidcUser g = mock(OidcUser.class);
        when(g.getEmail()).thenReturn(email);
        when(g.getEmailVerified()).thenReturn(verificado);
        when(g.getFullName()).thenReturn("Ana Pérez");
        when(g.getIdToken()).thenReturn(new OidcIdToken("tok", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "123456", "email", String.valueOf(email))));
        return g;
    }

    private String rolDe(OidcUser u) {
        return u.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("");
    }

    @Test
    void usuarioNuevoSeCreaConElRolQueEligioAlRegistrarse() {
        when(repo.findByUsername("ana@gmail.com")).thenReturn(Optional.empty());

        OidcUser u = servicio.vincularConUsuarioSgad(googleDice("Ana@Gmail.com", true), "ROLE_ENTRENADOR");

        assertEquals("ana@gmail.com", u.getName());
        assertEquals("ROLE_ENTRENADOR", rolDe(u));
        verify(repo).save(argThat(n -> "GOOGLE".equals(n.getProveedor())
                && "ana@gmail.com".equals(n.getUsername())
                && n.getPassword().startsWith("$2")        // hash BCrypt de una clave aleatoria, no texto plano
                && n.isHabilitado()));
        verify(perfil, never()).crearFicha(any());
    }

    @Test
    void deportistaNuevoRecibeSuFichaDeDeportista() {
        when(repo.findByUsername("ana@gmail.com")).thenReturn(Optional.empty());

        OidcUser u = servicio.vincularConUsuarioSgad(googleDice("ana@gmail.com", true), "ROLE_DEPORTISTA");

        assertEquals("ROLE_DEPORTISTA", rolDe(u));
        verify(perfil).crearFicha(any(Usuario.class));
    }

    @Test
    void sinRolElegidoUnCorreoNuevoSeRechazaYNoSeCrea() {
        when(repo.findByUsername("ana@gmail.com")).thenReturn(Optional.empty());

        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                () -> servicio.vincularConUsuarioSgad(googleDice("ana@gmail.com", true), null));

        assertEquals(GoogleOidcUserService.SIN_CUENTA, ex.getError().getErrorCode());
        verify(repo, never()).save(any());
    }

    @Test
    void nadieSePuedeRegistrarComoAdminPorGoogle() {
        when(repo.findByUsername("ana@gmail.com")).thenReturn(Optional.empty());

        assertThrows(OAuth2AuthenticationException.class,
                () -> servicio.vincularConUsuarioSgad(googleDice("ana@gmail.com", true), "ROLE_ADMIN"));
        verify(repo, never()).save(any());
    }

    @Test
    void usuarioExistenteConservaSuRolSinCrearOtro() {
        Usuario admin = new Usuario();
        admin.setUsername("jefe@gmail.com");
        admin.setRol("ROLE_ADMIN");
        admin.setHabilitado(true);
        when(repo.findByUsername("jefe@gmail.com")).thenReturn(Optional.of(admin));

        OidcUser u = servicio.vincularConUsuarioSgad(googleDice("jefe@gmail.com", true), null);

        assertEquals("ROLE_ADMIN", rolDe(u));
        verify(repo, never()).save(any());
    }

    @Test
    void rechazaCorreoSinVerificar() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> servicio.vincularConUsuarioSgad(googleDice("ana@gmail.com", false), "ROLE_DEPORTISTA"));
        verify(repo, never()).save(any());
    }

    @Test
    void rechazaUsuarioDeshabilitado() {
        Usuario u = new Usuario();
        u.setUsername("baneado@gmail.com");
        u.setRol("ROLE_DEPORTISTA");
        u.setHabilitado(false);
        when(repo.findByUsername("baneado@gmail.com")).thenReturn(Optional.of(u));

        assertThrows(OAuth2AuthenticationException.class,
                () -> servicio.vincularConUsuarioSgad(googleDice("baneado@gmail.com", true), null));
    }
}
