package com.sgad.service;

import com.sgad.entity.Escuela;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.repository.EscuelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EscuelaService {

    private final EscuelaRepository escuelaRepository;

    public List<Escuela> listarTodas() {
        return escuelaRepository.findAll();
    }

    public Optional<Escuela> buscarPorId(String id) {
        return escuelaRepository.findById(id);
    }

    public Escuela guardar(Escuela escuela) {
        return escuelaRepository.save(escuela);
    }

    public Escuela actualizar(String id, Escuela datos) {
        Escuela escuela = escuelaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escuela no encontrada con id: " + id));
        escuela.setNombre(datos.getNombre());
        escuela.setDireccion(datos.getDireccion());
        escuela.setTelefono(datos.getTelefono());
        // El arreglo de materiales NO se toca aquí: se administra desde
        // MaterialService, para no pisar el inventario al editar los
        // datos generales de la escuela.
        return escuelaRepository.save(escuela);
    }

    public void eliminar(String id) {
        if (!escuelaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Escuela no encontrada con id: " + id);
        }
        escuelaRepository.deleteById(id);
    }
}
