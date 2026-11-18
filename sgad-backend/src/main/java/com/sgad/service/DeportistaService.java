package com.sgad.service;

import com.sgad.entity.Deportista;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.exception.SolicitudInvalidaException;
import com.sgad.exception.Validar;
import com.sgad.repository.DeportistaRepository;
import com.sgad.repository.EscuelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeportistaService {

    private final DeportistaRepository deportistaRepository;
    private final EscuelaRepository escuelaRepository;

    public List<Deportista> listarTodos() {
        return deportistaRepository.findAll();
    }

    public List<Deportista> listarPorEscuela(String idEscuela) {
        return deportistaRepository.findByIdEscuela(idEscuela);
    }

    public List<Deportista> listarPorDeporte(String deporte) {
        return deportistaRepository.findByDeporte(deporte);
    }

    public Optional<Deportista> buscarPorId(String id) {
        return deportistaRepository.findById(id);
    }

    public Deportista guardar(Deportista deportista, String idEscuela) {
        Validar.idObligatorio(idEscuela, "idEscuela");
        exigirEscuela(idEscuela);
        deportista.setIdEscuela(idEscuela);
        return deportistaRepository.save(deportista);
    }

    public Deportista actualizar(String id, Deportista datos, String idEscuela) {
        Deportista deportista = deportistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Deportista no encontrado con id: " + id));
        deportista.setNombre(datos.getNombre());
        deportista.setEdad(datos.getEdad());
        deportista.setGenero(datos.getGenero());
        deportista.setDeporte(datos.getDeporte());
        if (Validar.informado(idEscuela)) {
            exigirEscuela(idEscuela);
            deportista.setIdEscuela(idEscuela);
        }
        return deportistaRepository.save(deportista);
    }

    public void eliminar(String id) {
        if (!deportistaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Deportista no encontrado con id: " + id);
        }
        deportistaRepository.deleteById(id);
    }

    private void exigirEscuela(String idEscuela) {
        if (!escuelaRepository.existsById(idEscuela)) {
            throw new SolicitudInvalidaException("La escuela indicada no existe (id: " + idEscuela + ").");
        }
    }
}
