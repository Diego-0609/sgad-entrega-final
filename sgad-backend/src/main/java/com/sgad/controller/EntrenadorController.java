package com.sgad.controller;

import com.sgad.dto.EntrenadorRequest;
import com.sgad.entity.Entrenador;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.EntrenadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entrenadores")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    @GetMapping
    public List<Entrenador> listar() {
        return entrenadorService.listarTodos();
    }

    @GetMapping("/escuela/{idEscuela}")
    public List<Entrenador> listarPorEscuela(@PathVariable String idEscuela) {
        return entrenadorService.listarPorEscuela(idEscuela);
    }

    @GetMapping("/{id}")
    public Entrenador buscarPorId(@PathVariable String id) {
        return entrenadorService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<Entrenador> crear(@Valid @RequestBody EntrenadorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entrenadorService.guardar(req.aEntidad(), req.idEscuela()));
    }

    @PutMapping("/{id}")
    public Entrenador actualizar(@PathVariable String id, @Valid @RequestBody EntrenadorRequest req) {
        return entrenadorService.actualizar(id, req.aEntidad(), req.idEscuela());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        entrenadorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
