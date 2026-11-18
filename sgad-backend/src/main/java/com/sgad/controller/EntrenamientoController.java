package com.sgad.controller;

import com.sgad.dto.EntrenamientoRequest;
import com.sgad.entity.Entrenamiento;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.EntrenamientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entrenamientos")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EntrenamientoController {

    private final EntrenamientoService entrenamientoService;

    @GetMapping
    public List<Entrenamiento> listar() {
        return entrenamientoService.listarTodos();
    }

    @GetMapping("/entrenador/{idEntrenador}")
    public List<Entrenamiento> listarPorEntrenador(@PathVariable String idEntrenador) {
        return entrenamientoService.listarPorEntrenador(idEntrenador);
    }

    @GetMapping("/{id}")
    public Entrenamiento buscarPorId(@PathVariable String id) {
        return entrenamientoService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenamiento no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<Entrenamiento> crear(@Valid @RequestBody EntrenamientoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entrenamientoService.guardar(req.aEntidad(), req.idEntrenador()));
    }

    @PutMapping("/{id}")
    public Entrenamiento actualizar(@PathVariable String id, @Valid @RequestBody EntrenamientoRequest req) {
        return entrenamientoService.actualizar(id, req.aEntidad(), req.idEntrenador());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        entrenamientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
