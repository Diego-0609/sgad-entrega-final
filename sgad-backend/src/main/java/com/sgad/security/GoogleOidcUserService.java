package com.sgad.security;

import com.sgad.entity.Usuario;
import com.sgad.repository.UsuarioRepository;
import com.sgad.service.PerfilService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Qué pasa DESPUÉS de que Google confirma quién es la persona.
 *
 * - Si el correo ya existe en MongoDB, entra con el rol que tiene guardado (entrenador, deportista...).
 * - Si no existe y venía de "Registrarme con Google", se crea con el rol que eligió en el registro.
 * - Si no existe y venía de "Iniciar sesión", se rechaza: primero debe registrarse y elegir su rol.
 *
 * El rol sale siempre de MongoDB o de las dos opciones permitidas; Google nunca da permisos de administrador.
 */
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    /** Atributo de sesión donde el registro deja el rol elegido antes de mandar al usuario a Google. */
    public static final String ROL_ELEGIDO = "sgad.rolGoogle";
    public static final Set<String> ROLES_AUTOREGISTRABLES = Set.of("ROLE_ENTRENADOR", "ROLE_DEPORTISTA");
    public static final String SIN_CUENTA = "sin_cuenta";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PerfilService perfilService;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        HttpSession sesion = sesionActual();
        String rolElegido = sesion == null ? null : (String) sesion.getAttribute(ROL_ELEGIDO);
        OidcUser usuario = vincularConUsuarioSgad(super.loadUser(request), rolElegido);
        if (sesion != null) {
            sesion.removeAttribute(ROL_ELEGIDO);
        }
        return usuario;
    }

    /** Separado de la llamada a Google para poder probarlo sin internet. */
    OidcUser vincularConUsuarioSgad(OidcUser google, String rolElegido) {
        String email = google.getEmail();
        if (email == null || !Boolean.TRUE.equals(google.getEmailVerified())) {
            throw error("email_no_verificado", "Google no confirmó que este correo esté verificado.");
        }
        String username = email.toLowerCase();

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseGet(() -> registrar(username, google.getFullName(), rolElegido));

        if (!usuario.isHabilitado()) {
            throw error("usuario_deshabilitado", "Tu usuario está deshabilitado.");
        }
        return new UsuarioGoogle(usuario.getUsername(), usuario.getRol(), google);
    }

    private Usuario registrar(String email, String nombre, String rol) {
        if (rol == null || !ROLES_AUTOREGISTRABLES.contains(rol)) {
            throw error(SIN_CUENTA, "Este correo todavía no tiene cuenta en el SGAD.");
        }
        Usuario u = new Usuario();
        u.setUsername(email);
        // Clave aleatoria que nadie conoce: esta cuenta solo entra por Google
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setHabilitado(true);
        u.setRol(rol);
        u.setProveedor("GOOGLE");
        u.setNombre(nombre);
        u = usuarioRepository.save(u);
        if ("ROLE_DEPORTISTA".equals(rol)) {
            perfilService.crearFicha(u);
        }
        return u;
    }

    private static OAuth2AuthenticationException error(String codigo, String mensaje) {
        return new OAuth2AuthenticationException(new OAuth2Error(codigo, mensaje, null));
    }

    private static HttpSession sesionActual() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra
                ? sra.getRequest().getSession(false) : null;
    }

    /** Usuario OIDC cuyo nombre (getName) es el username de MongoDB y cuyo rol sale de MongoDB. */
    static class UsuarioGoogle extends DefaultOidcUser {
        private final String username;

        UsuarioGoogle(String username, String rol, OidcUser google) {
            super(List.of(new SimpleGrantedAuthority(rol)), google.getIdToken(), google.getUserInfo());
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
