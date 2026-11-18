package com.sgad.dto;

import com.sgad.entity.Asistencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AsistenciaRequest(
        @NotBlank(message = "El estado es obligatorio.")
        @Pattern(regexp = "presente|ausente", message = "El estado debe ser 'presente' o 'ausente'.")
        String estado,

        String idDeportista,
        String idEntrenamiento
) {
    public Asistencia aEntidad() {
        Asistencia a = new Asistencia();
        a.setEstado(estado);
        return a;
    }
}
