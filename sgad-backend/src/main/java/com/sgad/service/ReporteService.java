package com.sgad.service;

import com.sgad.dto.ReporteRequest;
import com.sgad.entity.Reporte;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.repository.DeportistaRepository;
import com.sgad.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final DeportistaRepository deportistaRepository;

    public List<Reporte> listarTodos() {
        return reporteRepository.findAllByOrderByFechaDesc();
    }

    public List<Reporte> listarDe(String idDeportista) {
        return reporteRepository.findByIdDeportistaOrderByFechaDesc(idDeportista);
    }

    public Reporte crear(ReporteRequest req, String autor) {
        if (!deportistaRepository.existsById(req.idDeportista())) {
            throw new RecursoNoEncontradoException("Deportista no encontrado con id: " + req.idDeportista());
        }
        Reporte r = new Reporte();
        r.setIdDeportista(req.idDeportista());
        r.setAutor(autor);
        r.setTitulo(req.titulo().trim());
        r.setMensaje(req.mensaje().trim());
        r.setCalificacion(req.calificacion());
        r.setFecha(LocalDateTime.now(Zona.COLOMBIA));
        return reporteRepository.save(r);
    }

    public void eliminar(String id) {
        if (!reporteRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Reporte no encontrado con id: " + id);
        }
        reporteRepository.deleteById(id);
    }

    /** Solo el dueño del reporte puede marcarlo como leído; para otro deportista se responde como si no existiera. */
    public Reporte marcarLeido(String id, String idDeportista) {
        Reporte r = reporteRepository.findById(id)
                .filter(rep -> idDeportista.equals(rep.getIdDeportista()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado con id: " + id));
        r.setLeido(true);
        return reporteRepository.save(r);
    }

    public long noLeidos(String idDeportista) {
        return listarDe(idDeportista).stream().filter(r -> !r.isLeido()).count();
    }
}
