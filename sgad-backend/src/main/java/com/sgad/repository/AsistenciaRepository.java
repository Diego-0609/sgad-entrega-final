package com.sgad.repository;

import com.sgad.entity.Asistencia;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AsistenciaRepository extends MongoRepository<Asistencia, String> {
    List<Asistencia> findByIdDeportista(String idDeportista);
    List<Asistencia> findByIdEntrenamiento(String idEntrenamiento);
}
