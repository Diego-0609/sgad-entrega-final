package com.sgad.service;

import com.sgad.entity.Escuela;
import com.sgad.entity.MaterialItem;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.exception.SolicitudInvalidaException;
import com.sgad.exception.Validar;
import com.sgad.repository.EscuelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Material se decidió EMBEBER dentro de Escuela al migrar a MongoDB
 * (ver Escuela.java y MaterialItem.java), así que este servicio ya no
 * tiene su propia colección: lee y modifica el arreglo "materiales"
 * del documento Escuela correspondiente, pero conserva la misma API
 * REST que ya usaba el frontend Angular.
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final EscuelaRepository escuelaRepository;

    public List<MaterialItem> listarTodos() {
        List<MaterialItem> todos = new ArrayList<>();
        for (Escuela escuela : escuelaRepository.findAll()) {
            todos.addAll(conIdEscuela(escuela));
        }
        return todos;
    }

    public List<MaterialItem> listarPorEscuela(String idEscuela) {
        return escuelaRepository.findById(idEscuela)
                .map(this::conIdEscuela)
                .orElse(new ArrayList<>());
    }

    public Optional<MaterialItem> buscarPorId(String id) {
        for (Escuela escuela : escuelaRepository.findAll()) {
            for (MaterialItem m : conIdEscuela(escuela)) {
                if (id.equals(m.getIdMaterial())) {
                    return Optional.of(m);
                }
            }
        }
        return Optional.empty();
    }

    public MaterialItem guardar(MaterialItem material, String idEscuela) {
        Validar.idObligatorio(idEscuela, "idEscuela");
        Escuela escuela = escuelaRepository.findById(idEscuela)
                .orElseThrow(() -> new SolicitudInvalidaException("La escuela indicada no existe (id: " + idEscuela + ")."));
        material.setIdMaterial(UUID.randomUUID().toString());
        if (escuela.getMateriales() == null) {
            escuela.setMateriales(new ArrayList<>());
        }
        escuela.getMateriales().add(material);
        escuelaRepository.save(escuela);
        material.setIdEscuela(idEscuela);
        return material;
    }

    public MaterialItem actualizar(String id, MaterialItem datos, String idEscuela) {
        Escuela origen = escuelaRepository.findAll().stream()
                .filter(e -> e.getMateriales() != null
                        && e.getMateriales().stream().anyMatch(m -> id.equals(m.getIdMaterial())))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado con id: " + id));

        MaterialItem existente = origen.getMateriales().stream()
                .filter(m -> id.equals(m.getIdMaterial()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado con id: " + id));
        existente.setNombre(datos.getNombre());
        existente.setCantidad(datos.getCantidad());
        existente.setEstado(datos.getEstado());

        if (Validar.informado(idEscuela) && !idEscuela.equals(origen.getIdEscuela())) {
            // Mover el material de una escuela a otra: se quita del
            // arreglo de origen y se agrega al de destino.
            // Se valida el destino ANTES de tocar el origen, para no perder el material si falla.
            Escuela destino = escuelaRepository.findById(idEscuela)
                    .orElseThrow(() -> new SolicitudInvalidaException("La escuela indicada no existe (id: " + idEscuela + ")."));
            origen.getMateriales().removeIf(m -> id.equals(m.getIdMaterial()));
            escuelaRepository.save(origen);
            if (destino.getMateriales() == null) {
                destino.setMateriales(new ArrayList<>());
            }
            destino.getMateriales().add(existente);
            escuelaRepository.save(destino);
            existente.setIdEscuela(idEscuela);
        } else {
            escuelaRepository.save(origen);
            existente.setIdEscuela(origen.getIdEscuela());
        }
        return existente;
    }

    public void eliminar(String id) {
        for (Escuela escuela : escuelaRepository.findAll()) {
            if (escuela.getMateriales() != null
                    && escuela.getMateriales().removeIf(m -> id.equals(m.getIdMaterial()))) {
                escuelaRepository.save(escuela);
                return;
            }
        }
        throw new RecursoNoEncontradoException("Material no encontrado con id: " + id);
    }

    // Rellena el idEscuela (transitorio, no se guarda en Mongo) de cada
    // material de la escuela dada, para que la API mantenga el mismo
    // formato que antes usaba el frontend.
    private List<MaterialItem> conIdEscuela(Escuela escuela) {
        List<MaterialItem> lista = escuela.getMateriales() == null ? new ArrayList<>() : escuela.getMateriales();
        lista.forEach(m -> m.setIdEscuela(escuela.getIdEscuela()));
        return lista;
    }
}
