package com.sgad.controller;

import com.sgad.dto.DeportistaRequest;
import com.sgad.entity.Deportista;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.DeportistaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/deportistas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class DeportistaController {

    private final DeportistaService deportistaService;

    @GetMapping
    public List<Deportista> listar() {
        return deportistaService.listarTodos();
    }

    @GetMapping("/escuela/{idEscuela}")
    public List<Deportista> listarPorEscuela(@PathVariable String idEscuela) {
        return deportistaService.listarPorEscuela(idEscuela);
    }

    @GetMapping("/deporte/{deporte}")
    public List<Deportista> listarPorDeporte(@PathVariable String deporte) {
        return deportistaService.listarPorDeporte(deporte);
    }

    @GetMapping("/{id}")
    public Deportista buscarPorId(@PathVariable String id) {
        return deportistaService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Deportista no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<Deportista> crear(@Valid @RequestBody DeportistaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deportistaService.guardar(req.aEntidad(), req.idEscuela()));
    }

    @PutMapping("/{id}")
    public Deportista actualizar(@PathVariable String id, @Valid @RequestBody DeportistaRequest req) {
        return deportistaService.actualizar(id, req.aEntidad(), req.idEscuela());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        deportistaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
