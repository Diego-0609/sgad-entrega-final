package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Sesión de entrenamiento programada.
 *
 * Migración a MongoDB: colección propia ("entrenamientos") que
 * REFERENCIA a su entrenador por id. Los entrenamientos se acumulan
 * de forma indefinida (uno o más por día, durante años), así que se
 * mantienen aparte en vez de embeberse dentro de Entrenador.
 */
@Document(collection = "entrenamientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entrenamiento {

    @Id
    private String idEntrenamiento;

    private LocalDate fecha;
    private LocalTime hora;
    private String lugar;

    private String idEntrenador;
}
