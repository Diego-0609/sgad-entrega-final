package com.sgad.service;

import com.sgad.entity.Entrenamiento;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.exception.SolicitudInvalidaException;
import com.sgad.exception.Validar;
import com.sgad.repository.EntrenamientoRepository;
import com.sgad.repository.EntrenadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntrenamientoService {

    private final EntrenamientoRepository entrenamientoRepository;
    private final EntrenadorRepository entrenadorRepository;

    public List<Entrenamiento> listarTodos() {
        return entrenamientoRepository.findAll();
    }

    public List<Entrenamiento> listarPorEntrenador(String idEntrenador) {
        return entrenamientoRepository.findByIdEntrenador(idEntrenador);
    }

    public Optional<Entrenamiento> buscarPorId(String id) {
        return entrenamientoRepository.findById(id);
    }

    public Entrenamiento guardar(Entrenamiento entrenamiento, String idEntrenador) {
        Validar.idObligatorio(idEntrenador, "idEntrenador");
        exigirEntrenador(idEntrenador);
        entrenamiento.setIdEntrenador(idEntrenador);
        return entrenamientoRepository.save(entrenamiento);
    }

    public Entrenamiento actualizar(String id, Entrenamiento datos, String idEntrenador) {
        Entrenamiento entrenamiento = entrenamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenamiento no encontrado con id: " + id));
        entrenamiento.setFecha(datos.getFecha());
        entrenamiento.setHora(datos.getHora());
        entrenamiento.setLugar(datos.getLugar());
        if (Validar.informado(idEntrenador)) {
            exigirEntrenador(idEntrenador);
            entrenamiento.setIdEntrenador(idEntrenador);
        }
        return entrenamientoRepository.save(entrenamiento);
    }

    public void eliminar(String id) {
        if (!entrenamientoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Entrenamiento no encontrado con id: " + id);
        }
        entrenamientoRepository.deleteById(id);
    }

    private void exigirEntrenador(String idEntrenador) {
        if (!entrenadorRepository.existsById(idEntrenador)) {
            throw new SolicitudInvalidaException("El entrenador indicado no existe (id: " + idEntrenador + ").");
        }
    }
}
