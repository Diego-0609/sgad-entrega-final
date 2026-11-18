# SGAD — Actualización: Spring Security (login)

> **Ver también `README_SEGURIDAD_V2.md`**: permisos por rol en la API, administración de sesiones,
> control de errores y validaciones (versión final de la actividad de seguridad).

Este documento resume lo agregado al proyecto `sgad-backend` para la
actividad de seguridad, sin modificar el frontend Angular (que sigue
consumiendo la API REST en `/api/**`, ahora con sesión requerida).

> **Nota:** el backend migró de MySQL a MongoDB (ver
> `Actividad_Migracion_MongoDB/README.md` en la raíz del proyecto).
> Esta sección ya refleja esa migración.

## Qué se agregó

**Dependencias (`pom.xml`)**
- `spring-boot-starter-security`
- `spring-boot-starter-thymeleaf`
- `thymeleaf-extras-springsecurity6`

**Entidad**
- `Usuario` (username, password cifrado con BCrypt, habilitado, rol).
  El rol se guarda embebido como texto simple (`"ROLE_ADMIN"`, etc.)
  directamente en el documento del usuario — no existe una colección
  `Rol` aparte, porque un rol es solo una etiqueta fija sin atributos
  propios (ver justificación en `Actividad_Migracion_MongoDB/`).

**Repositorio**
- `UsuarioRepository` (`MongoRepository<Usuario, String>`)

**Seguridad (`com.sgad.security` / `com.sgad.config`)**
- `CustomUserDetailsService`: carga el usuario y su rol desde MongoDB
- `SecurityConfig`: define las reglas de acceso, login por formulario,
  logout y BCrypt como encoder
- `DataInitializer`: crea automáticamente al arrancar la app tres
  usuarios de prueba, solo si no existen:
  - `admin` / `admin123` → `ROLE_ADMIN`
  - `entrenador` / `entrenador123` → `ROLE_ENTRENADOR`
  - `deportista` / `deportista123` → `ROLE_DEPORTISTA`

**Páginas (Thymeleaf + Bootstrap, en `com.sgad.controller.PageController`)**
| Ruta | Acceso | Contenido |
|---|---|---|
| `/publico` | público | contadores generales (escuelas, entrenadores, deportistas) |
| `/login` | público | formulario de inicio de sesión |
| `/panel` | autenticado | resumen con los 6 contadores del proyecto |
| `/admin/zona` | solo `ADMIN` | tabla de escuelas y materiales reales |
| `/entrenador/zona` | `ADMIN` o `ENTRENADOR` | entrenadores, entrenamientos y deportistas reales |
| `/403` | — | página de acceso denegado |

Todas las tablas muestran **datos reales de la base de datos**, no
contenido de ejemplo.

## Formato de las entidades para el frontend Angular

Con MongoDB ya no hay relaciones bidireccionales `@ManyToOne`/`@OneToMany`
como en JPA: cada documento (`Entrenador`, `Deportista`, `Entrenamiento`,
`Asistencia`) simplemente guarda el id de su "padre" en un campo plano
(`idEscuela`, `idEntrenador`, etc.), igual que antes veía el frontend.
Por eso no hace falta `@JsonIgnore` ni getters adicionales: el propio
documento ya tiene la forma correcta para serializarse a JSON sin
ciclos.

## Base de datos

Con MongoDB no existe un script de creación de tablas ni un
`ddl-auto`: las colecciones (`escuelas`, `entrenadores`, `deportistas`,
`entrenamientos`, `asistencias`, `usuarios`) se crean solas la primera
vez que se guarda un documento en cada una. `DataInitializer` inserta
los usuarios de prueba automáticamente al primer arranque.

## Cómo probarlo

1. Levantar una instancia de MongoDB local (`mongod`, puerto por
   defecto `27017`) y correr el backend (`mvn spring-boot:run` o el
   botón Run de VS Code).
2. Ir a `http://localhost:8080/login`.
3. Entrar con `admin` / `admin123` (acceso completo),
   `entrenador` / `entrenador123` (sin la zona de administrador) o
   `deportista` / `deportista123`.
4. El frontend Angular (`http://localhost:4200`) ahora sí requiere
   sesión iniciada para consumir `/api/**` (ver `SecurityConfig`).
