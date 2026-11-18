# SGAD — Seguridad: autenticación, roles, sesiones y control de errores

> **Actualización v2:** JWT, login con Google, Docker y guía de servidor están en `README_JWT_OAUTH_DOCKER.md` (en la raíz del proyecto).

Este documento complementa a `README_SEGURIDAD.md` y explica lo agregado para
cumplir **todos** los parámetros de la actividad en grupo. Cada punto indica
en qué archivo está implementado.

## Cómo ejecutar

1. MongoDB local en el puerto 27017 (`mongod`).
2. Backend: `cd sgad-backend && mvn spring-boot:run`  → http://localhost:8080
3. Frontend: `cd sgad-frontend && npm install && npm start` → http://localhost:4200
4. Usuarios de prueba (se crean solos): `admin/admin123`, `entrenador/entrenador123`, `deportista/deportista123`.

Para probar la **expiración de sesión** sin esperar 15 minutos:
`SGAD_SESSION_TIMEOUT=1m mvn spring-boot:run` (en Windows PowerShell: `$env:SGAD_SESSION_TIMEOUT="1m"; mvn spring-boot:run`).

---

## 1. Autenticación en base de datos NoSQL y registro

| Requisito | Implementación |
|---|---|
| Usuarios en MongoDB con mínimo 2 roles | Colección `usuarios` (`entity/Usuario.java`). Roles: `ROLE_ADMIN`, `ROLE_ENTRENADOR`, `ROLE_DEPORTISTA`. |
| Contraseñas seguras | BCrypt (`SecurityConfig.passwordEncoder`). Nunca se guardan en texto plano. |
| Autenticación contra la BD | `security/CustomUserDetailsService.java` carga el usuario y su rol desde MongoDB. |
| Registro de usuarios | `GET/POST /registro` → `controller/RegistroController.java` + `templates/registro.html`. |
| Validaciones del registro (en el servidor) | Usuario 3–30 caracteres (letras, números, `. _ -`); contraseña 6–72; confirmación igual; rol solo ENTRENADOR/DEPORTISTA (**no** se puede autoregistrar un ADMIN); usuario repetido rechazado. |
| Usuario único garantizado por la BD | `@Indexed(unique = true)` en `Usuario.username` + `spring.data.mongodb.auto-index-creation=true`. |

## 2. Autorización (roles, permisos, rutas) y formulario de login

**Páginas del backend** (`SecurityConfig`):

| Ruta | Acceso |
|---|---|
| `/`, `/publico`, `/login`, `/registro` | público |
| `/panel` | cualquier usuario autenticado |
| `/entrenador/**` | ADMIN, ENTRENADOR |
| `/admin/**` | ADMIN |

**API REST `/api/**`** — antes solo exigía estar autenticado (un DEPORTISTA podía borrar escuelas llamando
directamente a la API). Ahora se aplica la siguiente matriz:

| Recurso | Leer (GET) | Crear / editar / borrar |
|---|---|---|
| `escuelas`, `entrenadores` | cualquier usuario | ADMIN |
| `deportistas`, `entrenamientos`, `asistencias` | cualquier usuario | ADMIN, ENTRENADOR |
| `materiales` | ADMIN, ENTRENADOR | ADMIN, ENTRENADOR |
| cualquier otra ruta `/api/**` no prevista | — | bloqueada (`denyAll`) |

El frontend refleja los mismos permisos (guards de Angular + botones ocultos), pero **la seguridad real está
en el backend**: los guards de Angular solo mejoran la experiencia.

**Formulario de login**: `.formLogin().loginPage("/login")` con `loginProcessingUrl("/login")`,
`failureUrl("/login?error=true")` y la plantilla `templates/login.html` (mensajes de error, sesión cerrada,
cuenta creada, sesión expirada y sesión reemplazada). Logout en `/logout` (acepta GET desde Angular y POST con
CSRF desde el panel), invalida la sesión y borra la cookie.

## 3. Administración de sesiones y control de errores

### Sesiones (`application.properties` + `SecurityConfig` + `security/SeguridadHandlers.java`)

| Aspecto | Configuración |
|---|---|
| Tiempo de vida | `server.servlet.session.timeout=15m` por **inactividad**; se renueva con cada petición. Cambiable con la variable `SGAD_SESSION_TIMEOUT`. |
| Cookie segura | `HttpOnly` (JavaScript no la puede leer) y `SameSite=Lax`; el id de sesión solo viaja en cookie (nunca en la URL). |
| Fijación de sesión | `sessionFixation().migrateSession()`: al iniciar sesión se cambia el id de la sesión. |
| Sesiones simultáneas | `maximumSessions(1)`: si el mismo usuario inicia sesión en otro lugar, la anterior se cierra (`sgad.session.max-por-usuario`). |
| Sesión expirada | API: `401` JSON `{"codigo":"SESION_EXPIRADA"}`; páginas: redirección a `/login?expired=true` con aviso. La cookie vencida se borra (sin bucles). |
| Frontend | La barra superior muestra una **cuenta regresiva** de la sesión; avisa 1 minuto antes y, al llegar a 0, envía al login con el aviso de sesión expirada (`auth.service.ts`, `error.interceptor.ts`). |

### Control de errores

* **Backend – un solo lugar**: `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`). Los controladores
  ya no tienen `try/catch`; lanzan `RecursoNoEncontradoException` (404) o `SolicitudInvalidaException` (400).
* **Validación de entrada** con Bean Validation (`dto/*Request.java`, `@Valid`): campos obligatorios, rangos
  (edad 1–120, cantidad ≥ 0), formatos de fecha/hora, estado de asistencia `presente|ausente`.
* **Formato único de error JSON** (`exception/ApiError.java`):
  `{timestamp, status, error, codigo, mensaje, ruta, errores{campo: mensaje}}`. También lo usan los errores de
  seguridad (401/403) y las rutas inexistentes (`exception/ApiErrorAttributes.java`).
* **Sin filtrar información interna**: los errores inesperados se registran en el log y al cliente solo se le
  dice un mensaje genérico (`server.error.include-stacktrace=never`).
* **Páginas de error** para las vistas Thymeleaf: `templates/error/403.html`, `404.html`, `500.html`, `4xx.html`
  y `templates/error.html`.
* **Frontend**: `interceptors/error.interceptor.ts` convierte cada error de la API en un aviso visible
  (`components/toast`), incluido el detalle por campo; 401 → login; 403 → "sin permisos"; sin conexión → aviso.

## 4. CRUD en colecciones (frontend + backend)

Seis colecciones/entidades con CRUD completo y su pantalla en Angular: **escuelas** (con **materiales** embebidos),
**entrenadores**, **deportistas**, **entrenamientos** y **asistencias** (más **usuarios** para el login).
Las referencias entre colecciones (`idEscuela`, `idEntrenador`, …) se validan antes de guardar.
Eliminar o editar algo que no existe responde 404 (antes "éxito" silencioso).

## 5. Preguntas probables al grupo

* *¿Dónde y cómo se guardan las contraseñas?* En la colección `usuarios`, con BCrypt (hash con sal; no reversible).
* *¿Por qué el rol va embebido en el usuario?* Es una etiqueta fija sin atributos propios; una colección aparte no aportaría nada.
* *¿Qué pasa si un DEPORTISTA llama directo a `DELETE /api/escuelas/{id}`?* Recibe `403 ACCESO_DENEGADO` (regla de `SecurityConfig`).
* *¿Cuánto dura la sesión y qué pasa al expirar?* 15 min de inactividad; la API responde 401 `SESION_EXPIRADA` y Angular manda al login con aviso.
* *¿Qué es la fijación de sesión y cómo se evita?* Un atacante fija un id de sesión antes del login; `migrateSession` lo cambia al autenticarse.
* *¿Por qué CSRF está desactivado en `/api/**`?* La API se usa desde Angular con cookie de sesión `SameSite=Lax` y CORS restringido al origen `http://localhost:4200`; los formularios del backend (login, registro, logout) sí llevan token CSRF.
* *¿Cómo viaja la sesión entre Angular (4200) y Spring (8080)?* Cookie `JSESSIONID` con `withCredentials` (interceptor) y CORS con credenciales.
* *¿Por qué `Material` va embebido y `Deportista` va como colección aparte?* Material es una lista pequeña y acotada que siempre se lee con su escuela; los deportistas pueden crecer mucho y son referenciados por asistencias.
* *¿Por qué hay validación en el HTML y también en el servidor?* Las del HTML se saltan con cualquier herramienta; la del servidor es la que protege.
