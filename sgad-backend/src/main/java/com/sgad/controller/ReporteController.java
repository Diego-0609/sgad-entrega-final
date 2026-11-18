package com.sgad.controller;

import com.sgad.dto.ReporteRequest;
import com.sgad.entity.Reporte;
import com.sgad.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reportes de rendimiento que envía el personal (ADMIN y ENTRENADOR). */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping
    public List<Reporte> listar() {
        return reporteService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Reporte> crear(@Valid @RequestBody ReporteRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.crear(req, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        reporteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
