package com.sgad.dto;

import com.sgad.entity.Entrenador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EntrenadorRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @NotBlank(message = "La especialidad es obligatoria.")
        @Size(max = 100, message = "La especialidad no puede superar 100 caracteres.")
        String especialidad,

        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "Teléfono no válido (7 a 20 dígitos).")
        String telefono,

        String idEscuela
) {
    public Entrenador aEntidad() {
        Entrenador e = new Entrenador();
        e.setNombre(nombre == null ? null : nombre.trim());
        e.setEspecialidad(especialidad);
        e.setTelefono(telefono);
        return e;
    }
}
