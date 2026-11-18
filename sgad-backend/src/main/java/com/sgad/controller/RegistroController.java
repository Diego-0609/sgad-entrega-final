package com.sgad.controller;

import com.sgad.entity.Usuario;
import com.sgad.repository.UsuarioRepository;
import com.sgad.security.GoogleOidcUserService;
import com.sgad.service.PerfilService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

/** Registro de cuentas: cualquiera puede crear una de Entrenador o Deportista; la de Admin no se autoregistra. */
@Controller
@RequiredArgsConstructor
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);
    private static final Pattern USUARIO_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 72; // límite real de BCrypt

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PerfilService perfilService;
    private final ObjectProvider<ClientRegistrationRepository> clientesOAuth;

    @GetMapping("/registro")
    public String formulario(@RequestParam(required = false) String sinCuenta, Model model) {
        model.addAttribute("googleHabilitado", clientesOAuth.getIfAvailable() != null);
        if (sinCuenta != null) {
            model.addAttribute("aviso", "Ese correo de Google todavía no tiene cuenta. "
                    + "Elige tu tipo de cuenta y pulsa «Registrarme con Google».");
        }
        return "registro";
    }

    /** Guarda el rol elegido en la sesión y manda al usuario a Google; al volver, GoogleOidcUserService crea la cuenta. */
    @GetMapping("/registro/google")
    public String registrarConGoogle(@RequestParam(defaultValue = "") String rol, HttpSession sesion) {
        if (clientesOAuth.getIfAvailable() == null || !GoogleOidcUserService.ROLES_AUTOREGISTRABLES.contains(rol)) {
            return "redirect:/registro";
        }
        sesion.setAttribute(GoogleOidcUserService.ROL_ELEGIDO, rol);
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/registro")
    public String registrar(
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false, defaultValue = "") String confirmarPassword,
            @RequestParam(required = false, defaultValue = "") String rol,
            Model model
    ) {
        username = username.trim();
        model.addAttribute("username", username);

        if (!USUARIO_VALIDO.matcher(username).matches()) {
            return error(model, "El usuario debe tener entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo.");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return error(model, "La contraseña debe tener entre " + PASSWORD_MIN + " y " + PASSWORD_MAX + " caracteres.");
        }
        if (!password.equals(confirmarPassword)) {
            return error(model, "Las contraseñas no coinciden.");
        }
        if (!rol.equals("ROLE_ENTRENADOR") && !rol.equals("ROLE_DEPORTISTA")) {
            return error(model, "Rol no válido.");
        }
        if (usuarioRepository.existsByUsername(username)) {
            return error(model, "Ese nombre de usuario ya está en uso.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(passwordEncoder.encode(password));
        nuevo.setHabilitado(true);
        nuevo.setRol(rol);

        try {
            nuevo = usuarioRepository.save(nuevo);
            if (rol.equals("ROLE_DEPORTISTA")) {
                perfilService.crearFicha(nuevo);
            }
        } catch (DuplicateKeyException e) {
            // Dos registros simultáneos con el mismo usuario: el índice único de MongoDB lo detiene.
            return error(model, "Ese nombre de usuario ya está en uso.");
        } catch (DataAccessException e) {
            log.error("No se pudo registrar al usuario '{}'", username, e);
            return error(model, "No fue posible crear la cuenta en este momento. Intenta de nuevo más tarde.");
        }

        return "redirect:/login?registrado=true";
    }

    private String error(Model model, String mensaje) {
        model.addAttribute("error", mensaje);
        model.addAttribute("googleHabilitado", clientesOAuth.getIfAvailable() != null);
        return "registro";
    }
}
