package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Deportista de una escuela.
 *
 * Migración a MongoDB: colección propia ("deportistas") que
 * REFERENCIA a su escuela por id. No se embebió dentro de Escuela
 * porque la lista de deportistas de una escuela puede crecer mucho
 * (decenas o cientos), y embeberla rompería el límite práctico de
 * tamaño de documento y obligaría a reescribir la escuela completa
 * cada vez que se registra un deportista nuevo.
 */
@Document(collection = "deportistas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deportista {

    @Id
    private String idDeportista;

    private String nombre;
    private Integer edad;
    private String genero;
    private String deporte;

    private String idEscuela;
}
