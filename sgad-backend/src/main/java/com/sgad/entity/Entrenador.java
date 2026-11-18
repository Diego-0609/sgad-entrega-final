package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entrenador de una escuela.
 *
 * Migración a MongoDB: se dejó como colección propia ("entrenadores"),
 * REFERENCIANDO a su escuela por id (idEscuela), en vez de embeberlo
 * dentro de Escuela. Razón: el número de entrenadores puede crecer, y
 * además cada entrenamiento referencia a su entrenador, así que
 * mantenerlo aparte evita duplicar sus datos en cada entrenamiento.
 */
@Document(collection = "entrenadores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entrenador {

    @Id
    private String idEntrenador;

    private String nombre;
    private String especialidad;
    private String telefono;

    // Referencia a la escuela dueña (equivalente al FK id_escuela de MySQL)
    private String idEscuela;
}
