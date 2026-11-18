package com.sgad.controller;

import com.sgad.entity.Escuela;
import com.sgad.entity.MaterialItem;
import com.sgad.repository.*;
import com.sgad.security.GoogleOidcUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

/** Páginas web (Thymeleaf) del backend: login, panel público y zonas por rol. */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final EscuelaRepository escuelaRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final DeportistaRepository deportistaRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ObjectProvider<ClientRegistrationRepository> clientesOAuth;

    @Value("${sgad.demo-users:true}")
    private boolean usuariosDemo;

    @GetMapping("/login")
    public String login(Model model, HttpSession sesion) {
        // Entrar por Google desde aquí no crea cuentas: se descarta cualquier rol elegido antes en el registro
        sesion.removeAttribute(GoogleOidcUserService.ROL_ELEGIDO);
        model.addAttribute("googleHabilitado", clientesOAuth.getIfAvailable() != null);
        model.addAttribute("mostrarDemo", usuariosDemo);
        return "login";
    }

    @GetMapping({"/", "/publico"})
    public String publico(Model model) {
        model.addAttribute("totalEscuelas", escuelaRepository.count());
        model.addAttribute("totalEntrenadores", entrenadorRepository.count());
        model.addAttribute("totalDeportistas", deportistaRepository.count());
        return "publico";
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        List<Escuela> escuelas = escuelaRepository.findAll();
        long totalMateriales = escuelas.stream()
                .mapToLong(e -> e.getMateriales() == null ? 0 : e.getMateriales().size())
                .sum();

        model.addAttribute("totalEscuelas", (long) escuelas.size());
        model.addAttribute("totalEntrenadores", entrenadorRepository.count());
        model.addAttribute("totalDeportistas", deportistaRepository.count());
        model.addAttribute("totalEntrenamientos", entrenamientoRepository.count());
        model.addAttribute("totalAsistencias", asistenciaRepository.count());
        model.addAttribute("totalMateriales", totalMateriales);
        return "panel";
    }

    @GetMapping("/admin/zona")
    public String zonaAdmin(Model model) {
        List<Escuela> escuelas = escuelaRepository.findAll();
        // Material vive embebido dentro de cada Escuela (ver Escuela.java);
        // para la vista se aplana en una sola lista.
        List<MaterialItem> materiales = escuelas.stream()
                .filter(e -> e.getMateriales() != null)
                .flatMap(e -> e.getMateriales().stream())
                .collect(Collectors.toList());
        model.addAttribute("escuelas", escuelas);
        model.addAttribute("materiales", materiales);
        return "admin-zona";
    }

    @GetMapping("/entrenador/zona")
    public String zonaEntrenador(Model model) {
        model.addAttribute("entrenadores", entrenadorRepository.findAll());
        model.addAttribute("entrenamientos", entrenamientoRepository.findAll());
        model.addAttribute("deportistas", deportistaRepository.findAll());
        return "entrenador-zona";
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "error/403";
    }
}
