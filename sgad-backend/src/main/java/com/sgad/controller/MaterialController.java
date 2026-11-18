package com.sgad.controller;

import com.sgad.dto.MaterialRequest;
import com.sgad.entity.MaterialItem;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/materiales")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public List<MaterialItem> listar() {
        return materialService.listarTodos();
    }

    @GetMapping("/escuela/{idEscuela}")
    public List<MaterialItem> listarPorEscuela(@PathVariable String idEscuela) {
        return materialService.listarPorEscuela(idEscuela);
    }

    @GetMapping("/{id}")
    public MaterialItem buscarPorId(@PathVariable String id) {
        return materialService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<MaterialItem> crear(@Valid @RequestBody MaterialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.guardar(req.aEntidad(), req.idEscuela()));
    }

    @PutMapping("/{id}")
    public MaterialItem actualizar(@PathVariable String id, @Valid @RequestBody MaterialRequest req) {
        return materialService.actualizar(id, req.aEntidad(), req.idEscuela());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        materialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
