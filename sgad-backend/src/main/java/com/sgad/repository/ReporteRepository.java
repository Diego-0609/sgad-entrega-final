package com.sgad.repository;

import com.sgad.entity.Reporte;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReporteRepository extends MongoRepository<Reporte, String> {
    List<Reporte> findByIdDeportistaOrderByFechaDesc(String idDeportista);
    List<Reporte> findAllByOrderByFechaDesc();
}
