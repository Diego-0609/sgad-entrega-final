package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro de asistencia de un deportista a un entrenamiento.
 *
 * Migración a MongoDB: es la relación con más crecimiento de todo el
 * sistema (un registro por deportista y por entrenamiento), así que se
 * mantiene como colección propia ("asistencias") que REFERENCIA tanto
 * al deportista como al entrenamiento, igual que la tabla asociativa
 * del modelo relacional. Embeberla en Entrenamiento o en Deportista
 * generaría un arreglo sin límite de crecimiento, algo que MongoDB
 * desaconseja explícitamente.
 */
@Document(collection = "asistencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asistencia {

    @Id
    private String idAsistencia;

    private String estado; // "presente" o "ausente"

    private String idDeportista;
    private String idEntrenamiento;
}
