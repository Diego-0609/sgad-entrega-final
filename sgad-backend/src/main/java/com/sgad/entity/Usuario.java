package com.sgad.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Usuario del sistema SGAD que puede autenticarse en el backend.
 * La contraseña se almacena siempre cifrada con BCrypt.
 *
 * Migración a MongoDB: el rol (antes una tabla "Rol" aparte,
 * referenciada por FK) se guarda aquí EMBEBIDO como texto simple
 * (ej. "ROLE_ADMIN"). Un rol no es más que una etiqueta fija sin
 * atributos propios y no crece de forma independiente, así que no se
 * justifica mantenerlo como colección ni como referencia: sería una
 * complejidad extra sin ningún beneficio.
 */
@Document(collection = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private String idUsuario;

    // Índice único: MongoDB impide dos usuarios con el mismo nombre aunque dos registros lleguen a la vez
    @Indexed(unique = true)
    private String username;
    private String password; // Hash BCrypt, nunca texto plano
    private boolean habilitado = true;
    private String rol; // Ej: "ROLE_ADMIN", "ROLE_ENTRENADOR", "ROLE_DEPORTISTA"

    // Cómo se crea/ingresa la cuenta: "LOCAL" (usuario y contraseña) o "GOOGLE" (OAuth2).
    // Los usuarios guardados antes de agregar este campo no lo tienen y se tratan como LOCAL.
    private String proveedor = "LOCAL";
    private String nombre; // Nombre completo (solo lo trae Google)

    // Perfil de deportista asociado a este usuario (solo para ROLE_DEPORTISTA)
    private String idDeportista;
}
