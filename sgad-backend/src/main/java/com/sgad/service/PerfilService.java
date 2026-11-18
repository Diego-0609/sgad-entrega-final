package com.sgad.service;

import com.sgad.entity.Deportista;
import com.sgad.entity.Usuario;
import com.sgad.exception.RecursoNoEncontradoException;
import com.sgad.repository.DeportistaRepository;
import com.sgad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Une la cuenta de un usuario DEPORTISTA con su ficha en la colección deportistas. */
@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final DeportistaRepository deportistaRepository;

    /** Devuelve la ficha del usuario; si todavía no tiene, la crea (el entrenador la completa después). */
    public synchronized Deportista deportistaDe(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        if (usuario.getIdDeportista() != null) {
            var existente = deportistaRepository.findById(usuario.getIdDeportista());
            if (existente.isPresent()) {
                return existente.get();
            }
        }
        return crearFicha(usuario);
    }

    public Deportista crearFicha(Usuario usuario) {
        Deportista ficha = new Deportista();
        boolean tieneNombre = usuario.getNombre() != null && !usuario.getNombre().isBlank();
        ficha.setNombre(tieneNombre ? usuario.getNombre() : usuario.getUsername());
        ficha = deportistaRepository.save(ficha);
        usuario.setIdDeportista(ficha.getIdDeportista());
        usuarioRepository.save(usuario);
        return ficha;
    }
}
