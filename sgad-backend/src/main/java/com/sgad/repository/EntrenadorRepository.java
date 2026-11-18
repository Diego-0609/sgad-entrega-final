package com.sgad.repository;

import com.sgad.entity.Entrenador;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EntrenadorRepository extends MongoRepository<Entrenador, String> {
    List<Entrenador> findByIdEscuela(String idEscuela);
}
