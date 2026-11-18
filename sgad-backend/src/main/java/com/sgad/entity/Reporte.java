package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Reporte de rendimiento que un entrenador envía a un deportista (llega a su buzón). */
@Document(collection = "reportes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {

    @Id
    private String idReporte;

    @Indexed
    private String idDeportista;
    private String autor;
    private String titulo;
    private String mensaje;
    private Integer calificacion;
    private LocalDateTime fecha;
    private boolean leido;
}
