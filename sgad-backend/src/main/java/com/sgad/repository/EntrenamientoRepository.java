package com.sgad.repository;

import com.sgad.entity.Entrenamiento;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EntrenamientoRepository extends MongoRepository<Entrenamiento, String> {
    List<Entrenamiento> findByIdEntrenador(String idEntrenador);
}
