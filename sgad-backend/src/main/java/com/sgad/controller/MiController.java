package com.sgad.controller;

import com.sgad.entity.Reporte;
import com.sgad.service.MiPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Panel personal del deportista. Solo ROLE_DEPORTISTA (ver SecurityConfig) y siempre sobre su propia ficha. */
@RestController
@RequestMapping("/api/mi")
@RequiredArgsConstructor
public class MiController {

    private final MiPanelService miPanel;

    @GetMapping("/perfil")
    public MiPanelService.Perfil perfil(Authentication auth) {
        return miPanel.perfil(auth.getName());
    }

    @GetMapping("/entrenamientos")
    public List<MiPanelService.EntrenamientoVista> entrenamientos(Authentication auth) {
        return miPanel.entrenamientos(auth.getName());
    }

    @GetMapping("/asistencias")
    public List<MiPanelService.EntrenamientoVista> asistencias(Authentication auth) {
        return miPanel.asistencias(auth.getName());
    }

    @GetMapping("/estadisticas")
    public MiPanelService.Estadisticas estadisticas(Authentication auth) {
        return miPanel.estadisticas(auth.getName());
    }

    @GetMapping("/reportes")
    public List<Reporte> reportes(Authentication auth) {
        return miPanel.reportes(auth.getName());
    }

    @PutMapping("/reportes/{id}/leido")
    public Reporte marcarLeido(@PathVariable String id, Authentication auth) {
        return miPanel.marcarReporteLeido(auth.getName(), id);
    }
}
