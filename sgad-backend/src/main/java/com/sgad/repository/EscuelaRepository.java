package com.sgad.repository;

import com.sgad.entity.Escuela;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EscuelaRepository extends MongoRepository<Escuela, String> {
}
