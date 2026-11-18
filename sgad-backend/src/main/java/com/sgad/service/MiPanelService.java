package com.sgad.service;

import com.sgad.entity.*;
import com.sgad.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Datos del panel personal del deportista: solo lo suyo, nunca el de otros. */
@Service
@RequiredArgsConstructor
public class MiPanelService {

    private final PerfilService perfilService;
    private final EscuelaRepository escuelaRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EstadisticasService estadisticasService;
    private final ReporteService reporteService;

    public record EntrenadorVista(String nombre, String especialidad, String telefono) {}

    public record Perfil(Deportista deportista, String escuela, List<EntrenadorVista> entrenadores, long reportesNoLeidos) {}

    public record EntrenamientoVista(String idEntrenamiento, LocalDate fecha, LocalTime hora, String lugar,
                                     String entrenador, String asistencia) {}

    public record Estadisticas(EstadisticasService.Resumen resumen, EntrenamientoVista proximo) {}

    public Perfil perfil(String username) {
        Deportista d = perfilService.deportistaDe(username);
        String escuela = d.getIdEscuela() == null ? null
                : escuelaRepository.findById(d.getIdEscuela()).map(Escuela::getNombre).orElse(null);
        List<EntrenadorVista> entrenadores = d.getIdEscuela() == null ? List.of()
                : entrenadorRepository.findByIdEscuela(d.getIdEscuela()).stream()
                .map(e -> new EntrenadorVista(e.getNombre(), e.getEspecialidad(), e.getTelefono()))
                .toList();
        return new Perfil(d, escuela, entrenadores, reporteService.noLeidos(d.getIdDeportista()));
    }

    /** Entrenamientos de los entrenadores de su escuela, más aquellos donde ya tiene asistencia registrada. */
    public List<EntrenamientoVista> entrenamientos(String username) {
        Deportista d = perfilService.deportistaDe(username);
        Map<String, Asistencia> asistencias = asistenciaRepository.findByIdDeportista(d.getIdDeportista()).stream()
                .collect(Collectors.toMap(Asistencia::getIdEntrenamiento, Function.identity(), (a, b) -> a));

        Map<String, Entrenamiento> entrenamientos = new LinkedHashMap<>();
        if (d.getIdEscuela() != null) {
            for (Entrenador e : entrenadorRepository.findByIdEscuela(d.getIdEscuela())) {
                entrenamientoRepository.findByIdEntrenador(e.getIdEntrenador())
                        .forEach(t -> entrenamientos.put(t.getIdEntrenamiento(), t));
            }
        }
        entrenamientoRepository.findAllById(asistencias.keySet())
                .forEach(t -> entrenamientos.put(t.getIdEntrenamiento(), t));

        Map<String, String> nombres = nombresDeEntrenadores(entrenamientos.values());
        return entrenamientos.values().stream()
                .map(t -> vista(t, nombres, asistencias.get(t.getIdEntrenamiento())))
                .sorted(masRecientePrimero())
                .toList();
    }

    /** Solo los entrenamientos con asistencia registrada (presente o ausente). */
    public List<EntrenamientoVista> asistencias(String username) {
        return entrenamientos(username).stream().filter(v -> v.asistencia() != null).toList();
    }

    public Estadisticas estadisticas(String username) {
        List<EntrenamientoVista> todos = entrenamientos(username);
        List<EstadisticasService.Registro> registros = todos.stream()
                .filter(v -> v.asistencia() != null)
                .map(v -> new EstadisticasService.Registro(v.fecha(), v.asistencia()))
                .toList();
        LocalDateTime ahora = LocalDateTime.now(Zona.COLOMBIA);
        EntrenamientoVista proximo = todos.stream()
                .filter(v -> v.fecha() != null
                        && !LocalDateTime.of(v.fecha(), v.hora() == null ? LocalTime.MIN : v.hora()).isBefore(ahora))
                .min(masRecientePrimero().reversed())
                .orElse(null);
        return new Estadisticas(estadisticasService.calcular(registros, ahora.toLocalDate()), proximo);
    }

    public List<Reporte> reportes(String username) {
        return reporteService.listarDe(perfilService.deportistaDe(username).getIdDeportista());
    }

    public Reporte marcarReporteLeido(String username, String idReporte) {
        return reporteService.marcarLeido(idReporte, perfilService.deportistaDe(username).getIdDeportista());
    }

    private Map<String, String> nombresDeEntrenadores(Collection<Entrenamiento> entrenamientos) {
        Set<String> ids = entrenamientos.stream().map(Entrenamiento::getIdEntrenador)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> nombres = new HashMap<>();
        entrenadorRepository.findAllById(ids).forEach(e -> nombres.put(e.getIdEntrenador(), e.getNombre()));
        return nombres;
    }

    private EntrenamientoVista vista(Entrenamiento t, Map<String, String> nombres, Asistencia a) {
        return new EntrenamientoVista(t.getIdEntrenamiento(), t.getFecha(), t.getHora(), t.getLugar(),
                nombres.get(t.getIdEntrenador()), a == null ? null : a.getEstado());
    }

    private Comparator<EntrenamientoVista> masRecientePrimero() {
        return Comparator.comparing(EntrenamientoVista::fecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(EntrenamientoVista::hora, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
