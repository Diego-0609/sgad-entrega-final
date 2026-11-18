package com.sgad.dto;

import com.sgad.entity.Entrenamiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record EntrenamientoRequest(
        @NotNull(message = "La fecha es obligatoria (formato yyyy-MM-dd).")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria (formato HH:mm).")
        LocalTime hora,

        @NotBlank(message = "El lugar es obligatorio.")
        @Size(max = 120, message = "El lugar no puede superar 120 caracteres.")
        String lugar,

        String idEntrenador
) {
    public Entrenamiento aEntidad() {
        Entrenamiento t = new Entrenamiento();
        t.setFecha(fecha);
        t.setHora(hora);
        t.setLugar(lugar == null ? null : lugar.trim());
        return t;
    }
}
