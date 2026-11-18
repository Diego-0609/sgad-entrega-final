package com.sgad.service;

import com.sgad.entity.Entrenador;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.exception.SolicitudInvalidaException;
import com.sgad.exception.Validar;
import com.sgad.repository.EntrenadorRepository;
import com.sgad.repository.EscuelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntrenadorService {

    private final EntrenadorRepository entrenadorRepository;
    private final EscuelaRepository escuelaRepository;

    public List<Entrenador> listarTodos() {
        return entrenadorRepository.findAll();
    }

    public List<Entrenador> listarPorEscuela(String idEscuela) {
        return entrenadorRepository.findByIdEscuela(idEscuela);
    }

    public Optional<Entrenador> buscarPorId(String id) {
        return entrenadorRepository.findById(id);
    }

    public Entrenador guardar(Entrenador entrenador, String idEscuela) {
        Validar.idObligatorio(idEscuela, "idEscuela");
        exigirEscuela(idEscuela);
        entrenador.setIdEscuela(idEscuela);
        return entrenadorRepository.save(entrenador);
    }

    public Entrenador actualizar(String id, Entrenador datos, String idEscuela) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id));
        entrenador.setNombre(datos.getNombre());
        entrenador.setEspecialidad(datos.getEspecialidad());
        entrenador.setTelefono(datos.getTelefono());
        if (Validar.informado(idEscuela)) {
            exigirEscuela(idEscuela);
            entrenador.setIdEscuela(idEscuela);
        }
        return entrenadorRepository.save(entrenador);
    }

    public void eliminar(String id) {
        if (!entrenadorRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Entrenador no encontrado con id: " + id);
        }
        entrenadorRepository.deleteById(id);
    }

    private void exigirEscuela(String idEscuela) {
        if (!escuelaRepository.existsById(idEscuela)) {
            throw new SolicitudInvalidaException("La escuela indicada no existe (id: " + idEscuela + ").");
        }
    }
}
