package com.sgad.config;

import com.sgad.entity.Usuario;
import com.sgad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea los usuarios de prueba al arrancar la aplicación, solo si todavía
 * no existen. El rol se guarda embebido como texto simple dentro del
 * propio Usuario (ver Usuario.java), así que aquí ya no hace falta una
 * colección de roles aparte.
 *
 * Usuarios de prueba:
 *   admin       / admin123        -> ROLE_ADMIN
 *   entrenador  / entrenador123   -> ROLE_ENTRENADOR
 *   deportista  / deportista123   -> ROLE_DEPORTISTA
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // En un servidor público: SGAD_USUARIOS_DEMO=false y SGAD_ADMIN_PASSWORD=<clave propia>
    @Value("${sgad.demo-users:true}")
    private boolean usuariosDemo;

    @Value("${sgad.admin-password:admin123}")
    private String claveAdmin;

    @Override
    public void run(String... args) {
        crearSiNoExiste("admin", claveAdmin, "ROLE_ADMIN");
        if (usuariosDemo) {
            crearSiNoExiste("entrenador", "entrenador123", "ROLE_ENTRENADOR");
            crearSiNoExiste("deportista", "deportista123", "ROLE_DEPORTISTA");
        }
    }

    private void crearSiNoExiste(String username, String password, String rol) {
        if (!usuarioRepository.existsByUsername(username)) {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setHabilitado(true);
            usuario.setRol(rol);
            usuarioRepository.save(usuario);
        }
    }
}
