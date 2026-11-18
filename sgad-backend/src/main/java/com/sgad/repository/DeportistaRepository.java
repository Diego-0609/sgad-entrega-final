package com.sgad.repository;

import com.sgad.entity.Deportista;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DeportistaRepository extends MongoRepository<Deportista, String> {
    List<Deportista> findByIdEscuela(String idEscuela);
    List<Deportista> findByDeporte(String deporte);
}
