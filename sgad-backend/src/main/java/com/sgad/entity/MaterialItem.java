package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Transient;

/**
 * Elemento de inventario deportivo (balones, petos, implementos, etc.).
 *
 * No es una colección propia de MongoDB: vive embebido dentro del
 * arreglo "materiales" de Escuela (ver Escuela.java), porque es una
 * lista pequeña y acotada que solo tiene sentido junto a su escuela
 * dueña.
 *
 * idEscuela es @Transient: NO se guarda dos veces dentro del documento
 * (sería redundante, ya está embebido dentro de esa misma escuela).
 * Solo se rellena al leer un material desde MaterialService, para que
 * la API siga entregando el mismo campo que ya usaba el frontend
 * Angular (mostrar a qué escuela pertenece cada material).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialItem {

    private String idMaterial;
    private String nombre;
    private Integer cantidad;
    private String estado;

    @Transient
    private String idEscuela;
}
