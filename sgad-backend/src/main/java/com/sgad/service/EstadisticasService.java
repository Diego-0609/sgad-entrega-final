package com.sgad.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcula las estadísticas de asistencia de un deportista (sin acceso a la base de datos). */
@Service
public class EstadisticasService {

    public static final String PRESENTE = "presente";
    private static final int SEMANAS_HISTORIAL = 4;

    public record Registro(LocalDate fecha, String estado) {}

    public record Semana(LocalDate desde, LocalDate hasta, int total, int presentes, int ausentes, int porcentaje) {}

    public record Resumen(int total, int presentes, int ausentes, int porcentaje, int racha,
                          Semana semanaActual, List<Semana> ultimasSemanas) {}

    public Resumen calcular(List<Registro> registros, LocalDate hoy) {
        List<Registro> pasados = registros.stream()
                .filter(r -> r.fecha() != null && !r.fecha().isAfter(hoy))
                .sorted(Comparator.comparing(Registro::fecha).reversed())
                .toList();

        int presentes = (int) pasados.stream().filter(r -> PRESENTE.equals(r.estado())).count();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Semana> semanas = new ArrayList<>();
        for (int i = SEMANAS_HISTORIAL - 1; i >= 0; i--) {
            semanas.add(semana(pasados, lunes.minusWeeks(i)));
        }
        return new Resumen(pasados.size(), presentes, pasados.size() - presentes, porcentaje(presentes, pasados.size()),
                racha(pasados), semanas.get(semanas.size() - 1), semanas);
    }

    private Semana semana(List<Registro> registros, LocalDate lunes) {
        LocalDate domingo = lunes.plusDays(6);
        List<Registro> deLaSemana = registros.stream()
                .filter(r -> !r.fecha().isBefore(lunes) && !r.fecha().isAfter(domingo))
                .toList();
        int presentes = (int) deLaSemana.stream().filter(r -> PRESENTE.equals(r.estado())).count();
        return new Semana(lunes, domingo, deLaSemana.size(), presentes, deLaSemana.size() - presentes,
                porcentaje(presentes, deLaSemana.size()));
    }

    /** Asistencias seguidas, contando desde la más reciente hacia atrás. */
    private int racha(List<Registro> masRecientePrimero) {
        int racha = 0;
        for (Registro r : masRecientePrimero) {
            if (!PRESENTE.equals(r.estado())) {
                break;
            }
            racha++;
        }
        return racha;
    }

    private int porcentaje(int parte, int total) {
        return total == 0 ? 0 : Math.round(parte * 100f / total);
    }
}
