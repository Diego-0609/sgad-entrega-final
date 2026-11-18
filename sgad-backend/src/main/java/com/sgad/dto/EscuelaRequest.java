package com.sgad.dto;

import com.sgad.entity.Escuela;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EscuelaRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @Size(max = 150, message = "La dirección no puede superar 150 caracteres.")
        String direccion,

        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "Teléfono no válido (7 a 20 dígitos).")
        String telefono
) {
    public Escuela aEntidad() {
        Escuela e = new Escuela();
        e.setNombre(nombre == null ? null : nombre.trim());
        e.setDireccion(direccion);
        e.setTelefono(telefono);
        return e;
    }
}
