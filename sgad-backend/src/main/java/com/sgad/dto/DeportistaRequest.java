package com.sgad.dto;

import com.sgad.entity.Deportista;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeportistaRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @NotNull(message = "La edad es obligatoria.")
        @Min(value = 1, message = "La edad debe ser mayor que 0.")
        @Max(value = 120, message = "La edad no puede superar 120.")
        Integer edad,

        @NotBlank(message = "El género es obligatorio.")
        String genero,

        @NotBlank(message = "El deporte es obligatorio.")
        @Size(max = 60, message = "El deporte no puede superar 60 caracteres.")
        String deporte,

        String idEscuela
) {
    public Deportista aEntidad() {
        Deportista d = new Deportista();
        d.setNombre(nombre == null ? null : nombre.trim());
        d.setEdad(edad);
        d.setGenero(genero);
        d.setDeporte(deporte);
        return d;
    }
}
