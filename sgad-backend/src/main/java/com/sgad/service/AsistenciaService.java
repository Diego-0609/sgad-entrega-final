package com.sgad.service;

import com.sgad.entity.Asistencia;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.exception.SolicitudInvalidaException;
import com.sgad.exception.Validar;
import com.sgad.repository.AsistenciaRepository;
import com.sgad.repository.DeportistaRepository;
import com.sgad.repository.EntrenamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final DeportistaRepository deportistaRepository;
    private final EntrenamientoRepository entrenamientoRepository;

    public List<Asistencia> listarTodas() {
        return asistenciaRepository.findAll();
    }

    public List<Asistencia> listarPorDeportista(String idDeportista) {
        return asistenciaRepository.findByIdDeportista(idDeportista);
    }

    public List<Asistencia> listarPorEntrenamiento(String idEntrenamiento) {
        return asistenciaRepository.findByIdEntrenamiento(idEntrenamiento);
    }

    public Optional<Asistencia> buscarPorId(String id) {
        return asistenciaRepository.findById(id);
    }

    public Asistencia guardar(Asistencia asistencia, String idDeportista, String idEntrenamiento) {
        Validar.idObligatorio(idDeportista, "idDeportista");
        Validar.idObligatorio(idEntrenamiento, "idEntrenamiento");
        if (!deportistaRepository.existsById(idDeportista)) {
            throw new SolicitudInvalidaException("El deportista indicado no existe (id: " + idDeportista + ").");
        }
        if (!entrenamientoRepository.existsById(idEntrenamiento)) {
            throw new SolicitudInvalidaException("El entrenamiento indicado no existe (id: " + idEntrenamiento + ").");
        }
        asistencia.setIdDeportista(idDeportista);
        asistencia.setIdEntrenamiento(idEntrenamiento);
        return asistenciaRepository.save(asistencia);
    }

    public Asistencia actualizar(String id, String nuevoEstado) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id));
        asistencia.setEstado(nuevoEstado);
        return asistenciaRepository.save(asistencia);
    }

    public void eliminar(String id) {
        if (!asistenciaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id);
        }
        asistenciaRepository.deleteById(id);
    }
}
