package com.sgad.controller;

import com.sgad.dto.AsistenciaRequest;
import com.sgad.entity.Asistencia;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.AsistenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @GetMapping
    public List<Asistencia> listar() {
        return asistenciaService.listarTodas();
    }

    @GetMapping("/deportista/{idDeportista}")
    public List<Asistencia> listarPorDeportista(@PathVariable String idDeportista) {
        return asistenciaService.listarPorDeportista(idDeportista);
    }

    @GetMapping("/entrenamiento/{idEntrenamiento}")
    public List<Asistencia> listarPorEntrenamiento(@PathVariable String idEntrenamiento) {
        return asistenciaService.listarPorEntrenamiento(idEntrenamiento);
    }

    @GetMapping("/{id}")
    public Asistencia buscarPorId(@PathVariable String id) {
        return asistenciaService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<Asistencia> crear(@Valid @RequestBody AsistenciaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.guardar(req.aEntidad(), req.idDeportista(), req.idEntrenamiento()));
    }

    @PutMapping("/{id}")
    public Asistencia actualizar(@PathVariable String id, @Valid @RequestBody AsistenciaRequest req) {
        return asistenciaService.actualizar(id, req.estado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        asistenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
