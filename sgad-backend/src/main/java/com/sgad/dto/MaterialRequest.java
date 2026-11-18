package com.sgad.dto;

import com.sgad.entity.MaterialItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 0, message = "La cantidad no puede ser negativa.")
        Integer cantidad,

        @NotBlank(message = "El estado es obligatorio.")
        String estado,

        String idEscuela
) {
    public MaterialItem aEntidad() {
        MaterialItem m = new MaterialItem();
        m.setNombre(nombre == null ? null : nombre.trim());
        m.setCantidad(cantidad);
        m.setEstado(estado);
        return m;
    }
}
