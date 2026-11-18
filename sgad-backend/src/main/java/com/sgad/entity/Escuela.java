package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Escuela deportiva. Entidad raíz del modelo: cada escuela es dueña de
 * sus entrenadores, deportistas y materiales.
 *
 * Migración a MongoDB: Material se decidió EMBEBER aquí (arreglo
 * "materiales") porque es una lista pequeña, acotada, que ninguna otra
 * colección referencia y que siempre se consulta junto con su escuela.
 * Entrenadores y deportistas, en cambio, se dejaron como colecciones
 * aparte (ver Entrenador.java y Deportista.java) porque pueden crecer
 * mucho y sí son referenciados desde otras colecciones.
 */
@Document(collection = "escuelas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Escuela {

    @Id
    private String idEscuela;

    private String nombre;
    private String direccion;
    private String telefono;

    private List<MaterialItem> materiales = new ArrayList<>();
}
