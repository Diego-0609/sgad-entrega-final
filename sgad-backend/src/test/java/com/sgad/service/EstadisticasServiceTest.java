package com.sgad.service;

import com.sgad.service.EstadisticasService.Registro;
import com.sgad.service.EstadisticasService.Resumen;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasServiceTest {

    // Miércoles 4 de noviembre de 2026: su semana va del lunes 2 al domingo 8
    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);
    private final EstadisticasService servicio = new EstadisticasService();

    private Registro r(String fecha, String estado) {
        return new Registro(LocalDate.parse(fecha), estado);
    }

    @Test
    void sinRegistrosTodoEsCero() {
        Resumen res = servicio.calcular(List.of(), HOY);

        assertEquals(0, res.total());
        assertEquals(0, res.porcentaje());
        assertEquals(0, res.racha());
        assertEquals(4, res.ultimasSemanas().size());
    }

    @Test
    void calculaTotalesYPorcentaje() {
        Resumen res = servicio.calcular(List.of(
                r("2026-11-02", "presente"), r("2026-11-03", "presente"),
                r("2026-10-30", "ausente"), r("2026-10-28", "presente")), HOY);

        assertEquals(4, res.total());
        assertEquals(3, res.presentes());
        assertEquals(1, res.ausentes());
        assertEquals(75, res.porcentaje());
    }

    @Test
    void laSemanaActualVaDeLunesADomingo() {
        Resumen res = servicio.calcular(List.of(
                r("2026-11-02", "presente"), r("2026-11-03", "ausente"),
                r("2026-11-01", "presente")), HOY);           // domingo anterior: ya es otra semana

        assertEquals(LocalDate.of(2026, 11, 2), res.semanaActual().desde());
        assertEquals(LocalDate.of(2026, 11, 8), res.semanaActual().hasta());
        assertEquals(2, res.semanaActual().total());
        assertEquals(1, res.semanaActual().presentes());
        assertEquals(50, res.semanaActual().porcentaje());
    }

    @Test
    void laRachaSeCortaConLaPrimeraAusenciaMasReciente() {
        Resumen res = servicio.calcular(List.of(
                r("2026-10-26", "presente"), r("2026-10-28", "ausente"),
                r("2026-10-30", "presente"), r("2026-11-02", "presente"), r("2026-11-03", "presente")), HOY);

        assertEquals(3, res.racha());
    }

    @Test
    void ignoraEntrenamientosFuturos() {
        Resumen res = servicio.calcular(List.of(r("2026-11-10", "ausente"), r("2026-11-03", "presente")), HOY);

        assertEquals(1, res.total());
        assertEquals(100, res.porcentaje());
    }
}
