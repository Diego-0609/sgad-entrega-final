package com.sgad.controller;

import com.sgad.dto.EscuelaRequest;
import com.sgad.entity.Escuela;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.EscuelaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Los errores (404, 400, 500...) NO se manejan aquí: los resuelve
 * GlobalExceptionHandler. Los permisos (quién puede crear/editar/borrar)
 * se definen en SecurityConfig.
 */
@RestController
@RequestMapping("/api/escuelas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EscuelaController {

    private final EscuelaService escuelaService;

    @GetMapping
    public List<Escuela> listar() {
        return escuelaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Escuela buscarPorId(@PathVariable String id) {
        return escuelaService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escuela no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<Escuela> crear(@Valid @RequestBody EscuelaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escuelaService.guardar(req.aEntidad()));
    }

    @PutMapping("/{id}")
    public Escuela actualizar(@PathVariable String id, @Valid @RequestBody EscuelaRequest req) {
        return escuelaService.actualizar(id, req.aEntidad());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        escuelaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
