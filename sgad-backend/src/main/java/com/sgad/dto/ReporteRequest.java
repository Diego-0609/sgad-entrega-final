package com.sgad.dto;

import jakarta.validation.constraints.*;

public record ReporteRequest(
        @NotBlank(message = "Selecciona un deportista.")
        String idDeportista,
        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 100, message = "El título no puede superar 100 caracteres.")
        String titulo,
        @NotBlank(message = "El mensaje es obligatorio.")
        @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres.")
        String mensaje,
        @Min(value = 1, message = "La calificación va de 1 a 5.")
        @Max(value = 5, message = "La calificación va de 1 a 5.")
        Integer calificacion
) {}
