# SGAD v2: JWT + Google (OAuth2) + Docker + Servidor

Lo nuevo sobre la versión anterior (MongoDB, roles, sesiones, errores). **Todo lo anterior sigue funcionando igual**: las 75 pruebas del backend y las 18 del navegador pasan igual que antes.

| Pedido | Estado | Dónde está |
|---|---|---|
| JSON Web Token | Hecho. **Convive con la cookie de sesión** y **Angular ya lo usa** | `security/JwtService`, `JwtAuthFilter`, `controller/TokenController`, `config/SecurityConfig`; en Angular `interceptors/jwt.interceptor.ts` y `services/auth.service.ts` |
| OAuth 2 con Google | Hecho (necesita tus credenciales de Google, sección 2) | `config/GoogleOAuthConfig`, `security/GoogleOidcUserService`, `login.html` |
| Investigar y aplicar Docker | Hecho | `docker-compose.yml`, `sgad-backend/Dockerfile`, `sgad-frontend/Dockerfile`, `nginx.conf` |
| Agregar a un servidor | Guía lista (sección 4). Falta que tú lo ejecutes en tu servidor | esta guía |

---

## 1. JWT (JSON Web Token)

**Idea simple:** una manilla de concierto. Muestras tu cédula una sola vez (usuario y contraseña) y te dan una manilla (el token). Después solo enseñas la manilla; ya no hace falta la cédula. La manilla trae escrito quién eres y cuándo vence, y tiene un sello que solo el organizador sabe hacer (la firma). Si alguien le cambia lo escrito, el sello ya no coincide y la rechazan.

**Cómo es un token:** tres partes separadas por puntos, `cabecera.contenido.firma`. El contenido lleva `sub` (usuario), `rol`, `iat` (cuándo se creó) y `exp` (cuándo vence). Se puede leer (no es secreto), pero no se puede modificar sin romper la firma. Por eso **nunca lleva la contraseña**.

### Cómo convive con la cookie

Antes el sistema solo tenía **sesión con cookie** (`JSESSIONID`). Ahora hay **dos cadenas de seguridad** y Spring escoge según la petición:

```
Petición a /api/**  con  "Authorization: Bearer <token>"  ──▶  Cadena 1 (JWT): sin sesión, sin cookie
Cualquier otra cosa (formulario, Google, páginas, API con cookie) ──▶  Cadena 2 (sesión/cookie): como siempre
```

La **matriz de permisos por rol es la misma** para las dos (`permisosDeLaApi` en `SecurityConfig`): un DEPORTISTA con JWT tampoco puede crear escuelas.

### Cómo se usa

```bash
# 1) Pedir el token (usuario + contraseña)
curl -X POST http://localhost:8080/api/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
# → {"accessToken":"eyJhbGci...","tokenType":"Bearer","expiresIn":3600,"username":"admin","rol":"ROLE_ADMIN"}

# 2) Usarlo
curl http://localhost:8080/api/escuelas -H "Authorization: Bearer eyJhbGci..."

# 3) Ver con qué se autenticó la petición
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer eyJhbGci..."   # "metodo": "JWT"
```

Quien ya inició sesión en la web (formulario o Google) puede cambiar su sesión por un token con `GET /api/auth/token`.

### Errores que devuelve (JSON con `codigo`)

| Situación | HTTP | `codigo` |
|---|---|---|
| Sin token ni cookie | 401 | `NO_AUTENTICADO` |
| Token vencido | 401 | `TOKEN_EXPIRADO` |
| Token alterado, firmado con otra clave, sin firma (`alg=none`), de otro emisor, sin `exp`, o de un usuario que ya no existe | 401 | `TOKEN_INVALIDO` |
| Usuario o clave incorrectos al pedir el token | 401 | `CREDENCIALES_INVALIDAS` (el mismo mensaje exista o no el usuario) |
| Rol sin permiso | 403 | `ACCESO_DENEGADO` |

### Decisiones de seguridad (para explicar en la sustentación)

- **La clave que firma no está en el código:** viene de `SGAD_JWT_SECRET` (mínimo 32 caracteres; si es más corta la app no arranca).
- **En cada petición se vuelve a leer el usuario en MongoDB.** Si un admin deshabilita a alguien o le cambia el rol, el token existente ya lo refleja. Es un poco menos "puro" que un JWT sin consulta, pero es más seguro.
- **Un JWT no puede pedir otro JWT** (`/api/auth/token` está bloqueado con Bearer). Si no, un token robado se podría renovar para siempre.
- Con JWT **no hay CSRF** porque el navegador no manda el token solo; con cookie sí hay que protegerse (ya estaba).

### Cómo lo usa Angular

1. El usuario inicia sesión como siempre (formulario o Google) → queda con **cookie**.
2. Angular pregunta quién es (`/api/auth/me`, con cookie) y **cambia la sesión por un JWT** (`GET /api/auth/token`, con cookie). El token se guarda **solo en memoria** (no en `localStorage`: así un script malicioso no lo puede leer del disco del navegador; al recargar la página se vuelve a pedir con la cookie).
3. Desde ahí, **cada petición de datos** (`/api/escuelas`, etc.) sale con `Authorization: Bearer <token>` (`JwtInterceptor`).
4. **El token dura lo mismo que la sesión** (15 min por defecto). Cuando le queda menos de la mitad, Angular lo **renueva con la cookie** antes de que venza; esa renovación además mantiene viva la sesión mientras el usuario esté activo.
5. Si el usuario se queda inactivo, la sesión muere; en la siguiente renovación el servidor responde 401 y Angular manda al login con el aviso de "sesión expirada".

Interruptor: en `src/environments/environment.ts` (y `environment.prod.ts`), `useJwt: false` vuelve a usar **solo la cookie**, como en la versión anterior.

**Límites que conviene conocer:**
- Cerrar sesión mata la cookie, pero **un JWT ya emitido sigue valiendo hasta que vence** (así funciona JWT; por eso vive poco).
- **La regla "una sesión por usuario" se aplica con retraso en Angular:** si el mismo usuario inicia sesión en otro lugar, la pestaña vieja sigue funcionando con su token hasta que intente renovarlo (máximo ~7 minutos con los valores por defecto) y entonces recibe "sesión reemplazada". Con la cookie sola (`useJwt: false`) era inmediato.
- No hay *refresh token* aparte ni lista de tokens revocados: la renovación depende de la sesión (cookie).

---

## 2. Login con Google (OAuth 2.0 / OpenID Connect)

**Idea simple:** "Entrar con tu cédula de Google". El SGAD **nunca ve tu contraseña de Google**. Te manda a Google, tú te identificas allá, y Google le responde al SGAD: "esta persona es ana@gmail.com y su correo está verificado". Es como cuando un amigo responde por ti en la puerta.

### El flujo, paso a paso

1. Haces clic en **Continuar con Google** → el navegador va a `/oauth2/authorization/google`.
2. El SGAD te redirige a `accounts.google.com` con su `client_id`, los permisos que pide (`openid profile email`) y un `state` aleatorio (contra falsificaciones).
3. Te identificas en Google y aceptas.
4. Google te devuelve al SGAD en `/login/oauth2/code/google` con un **código** de un solo uso.
5. El SGAD (servidor a servidor, con su `client_secret`) cambia ese código por el **ID token** que dice quién eres.
6. `GoogleOidcUserService` busca ese correo en MongoDB: si existe usa su rol; si no existe lo crea como **`ROLE_DEPORTISTA`** (el de menor privilegio). Un admin puede subirlo cambiando el campo `rol` en la colección `usuarios`.
7. Quedas con **sesión normal** (cookie) y te lleva al frontend.

Detalles de seguridad: el correo debe venir **verificado** por Google; el usuario se guarda con su correo, y como el registro normal **no permite `@`** en el usuario, nadie puede crearse antes una cuenta local "robando" ese nombre.

### Crear las credenciales (lo único que debes hacer tú)

1. Entra a <https://console.cloud.google.com> → crea un proyecto (ej. "SGAD").
2. **APIs y servicios → Pantalla de consentimiento de OAuth** → tipo *Externo* → nombre de la app y tu correo. Mientras esté en modo *Prueba*, agrega tu correo (y el de tus compañeros) en **Usuarios de prueba**; solo ellos podrán entrar.
3. **Credenciales → Crear credenciales → ID de cliente de OAuth → Aplicación web.**
4. En **URI de redireccionamiento autorizados** agrega **exactamente**:
   - Local sin Docker: `http://localhost:8080/login/oauth2/code/google`
   - Local con Docker: `http://localhost/login/oauth2/code/google`
   - Servidor: `https://tudominio.com/login/oauth2/code/google`
5. Copia el **Client ID** y el **Client Secret**.

> ⚠️ Google **exige HTTPS** (excepto `localhost`) y **no acepta direcciones IP** como redirect. Para que Google funcione en un servidor necesitas un **dominio con HTTPS** (sección 4).

### Activarlo

```bash
# Sin Docker (PowerShell):
$env:SGAD_GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"; $env:SGAD_GOOGLE_CLIENT_SECRET="xxxx"; mvn spring-boot:run
# Con Docker: ponlos en el archivo .env
```

Sin esas variables **la app funciona igual** y simplemente no muestra el botón. **Nunca** pegues el secret en el código ni lo subas a GitHub.

---

## 3. Docker

**Idea simple:** un **contenedor** es como un tupper sellado: lleva dentro el programa *y* todo lo que necesita (Java, Node, MongoDB), así que funciona igual en tu PC, en la del compañero y en el servidor. "En mi PC sí funciona" deja de ser problema.

| Concepto | Qué es | Ejemplo aquí |
|---|---|---|
| **Imagen** | La receta/plantilla (solo lectura) | la que construye `sgad-backend/Dockerfile` |
| **Contenedor** | Una imagen ejecutándose | `backend`, `frontend`, `mongo` |
| **Dockerfile** | Las instrucciones para construir una imagen | uno por servicio |
| **Volumen** | Disco que sobrevive al contenedor | `mongo_data` (si recreas el contenedor, **los datos no se pierden**) |
| **Docker Compose** | Levanta varios contenedores juntos con un solo comando | `docker-compose.yml` |

### Qué hay en el sistema

```
Navegador ──▶ [ frontend: nginx + Angular ] ──▶ [ backend: Spring Boot ] ──▶ [ mongo ]
               único puerto expuesto (80)        red interna                  red interna (sin puerto público)
```

- **`sgad-backend/Dockerfile`** — *multi-stage*: la etapa 1 (Maven) compila y corre las pruebas unitarias; la etapa 2 solo copia el `.jar` a una imagen pequeña con Java, **sin root**. Copiar primero el `pom.xml` hace que las dependencias queden en caché.
- **`sgad-frontend/Dockerfile`** — etapa 1 (Node) compila Angular con `environment.prod.ts`; etapa 2 (nginx) sirve los archivos.
- **`nginx.conf`** — la "puerta de entrada": `/` es Angular; `/api`, `/login`, `/oauth2`... se reenvían al backend. Así el navegador ve **un solo dominio** y no hay problemas de CORS ni de cookies. También pasa `X-Forwarded-Proto/Host` para que Google reciba la URL pública correcta.
- **`docker-compose.yml`** — define los 3 servicios; el backend espera a que MongoDB esté sano (`healthcheck`) antes de arrancar.
- **`.env`** — los secretos y la configuración (copia de `.env.example`). Está en `.gitignore`.

### Ejecutarlo (en tu PC con Docker Desktop)

```bash
cp .env.example .env            # Windows: copy .env.example .env
# edita .env: pon SGAD_JWT_SECRET (genera una con:  openssl rand -base64 48)
docker compose up -d --build
# abre http://localhost        (usuarios demo: admin/admin123, entrenador/entrenador123, deportista/deportista123)
docker compose logs -f backend   # ver logs
docker compose down              # apagar (los datos se conservan)
docker compose down -v           # apagar Y BORRAR la base de datos
```

---

## 4. Subirlo a un servidor (VPS con Ubuntu)

Sirve cualquier VPS (DigitalOcean, Hetzner, Oracle Cloud gratis, etc.). Mínimo recomendado: 2 GB de RAM.

```bash
# 1) En el servidor: instalar Docker
curl -fsSL https://get.docker.com | sh

# 2) Copiar el proyecto (git clone de tu repo, o scp del zip) y entrar a la carpeta
cd SGAD-proyecto-completo
cp .env.example .env && nano .env

# 3) Levantar
docker compose up -d --build
```

**Qué poner en `.env` para un servidor público:**

```
SGAD_JWT_SECRET=<una clave larga y propia>
SGAD_USUARIOS_DEMO=false            # no crear entrenador/deportista con claves públicas
SGAD_ADMIN_PASSWORD=<clave fuerte>  # solo se aplica si "admin" aún no existe en MongoDB
SGAD_FRONTEND_URL=https://tudominio.com
SGAD_COOKIE_SECURE=true
```

**Firewall:** abre solo 22 (SSH), 80 y 443. MongoDB y el backend **no** se publican al exterior (ya está así en el compose).

**HTTPS (necesario para Google):** apunta tu dominio al servidor y pon **Caddy** delante (consigue el certificado solo):

```bash
# En .env:  SGAD_HTTP_PORT=8080  y  SGAD_BIND_IP=127.0.0.1   (solo Caddy puede llegar al SGAD; Docker se salta el firewall ufw)
sudo apt install -y caddy
# /etc/caddy/Caddyfile  →
#   tudominio.com {
#       reverse_proxy localhost:8080
#   }
sudo systemctl reload caddy
```

**Copia de seguridad de la base de datos:**
`docker compose exec mongo mongodump --archive --db sgad > respaldo.archive`

**Actualizar después de cambiar código:** `git pull && docker compose up -d --build`.

---

## 5. Qué se probó (y qué no)

| Prueba | Resultado |
|---|---|
| Unitarias Java (JWT + puente Google→MongoDB) | 12/12 |
| `pruebas_jwt_google.py` (servidor real + MongoDB) | 61/61 |
| `pruebas_angular_jwt.py` (navegador real: Angular manda el Bearer y renueva el token) | 9/9 |
| `pruebas_backend.py` (las 75 de antes, repetidas) | 75/75 |
| `pruebas_frontend.py` (navegador real, 18 de antes) | 18/18 |
| Extremo a extremo **a través de nginx** (login, Angular con JWT, rutas, cookie HttpOnly, redirect de Google) | OK |

Lo que **no** pude comprobar desde mi entorno, para que lo tengas claro:

- **Docker no se ejecutó aquí** (no había motor de Docker). Sí validé `docker compose config`, que el `nginx.conf` es válido y funciona real, y que el `.jar` y el Angular de producción compilan. El primer `docker compose up --build` en tu PC es la prueba final de los Dockerfile.
- **El login con Google real** necesita tus credenciales y una cuenta de Google. Probé todo lo demás: el botón, la redirección a Google con los parámetros correctos, el rechazo de un callback falso y la lógica del usuario (con pruebas unitarias).
- **No lo desplegué en un servidor real.**
- En una pasada de las 18 pruebas de navegador falló una vez la de "DEPORTISTA entrando por URL a /escuelas" (me mandó al login por sesión expirada). No se repitió en 4 corridas siguientes (18/18 cada vez) y no encontré la causa; si te pasa, avísame con el momento exacto.

## 6. Variables de entorno

| Variable | Para qué | Por defecto |
|---|---|---|
| `SGAD_JWT_SECRET` | Clave que firma los JWT (≥ 32 caracteres) | solo de desarrollo (avisa en el log) |
| `SGAD_JWT_EXPIRATION` | Vida del token en segundos | `3600` |
| `SGAD_GOOGLE_CLIENT_ID` / `_SECRET` | Activan el login con Google | vacío = desactivado |
| `SGAD_MONGO_URI` | Conexión a MongoDB | `mongodb://localhost:27017/sgad` |
| `SGAD_FRONTEND_URL` | Dirección pública (redirección tras login y CORS) | `http://localhost:4200` |
| `SGAD_COOKIE_SECURE` | Cookie solo por HTTPS | `false` |
| `SGAD_SESSION_TIMEOUT` | Vida de la sesión por inactividad | `15m` |
| `SGAD_USUARIOS_DEMO` / `SGAD_ADMIN_PASSWORD` | Usuarios iniciales | `true` / `admin123` |

## 7. Preguntas que te pueden hacer

- **¿Qué diferencia hay entre sesión y JWT?** La sesión guarda el estado en el servidor y el navegador lleva una cookie con un id. El JWT lleva el estado *dentro* del token firmado; el servidor solo verifica la firma.
- **¿Por qué no se puede falsificar?** Porque la firma depende de una clave que solo conoce el servidor; si cambias el contenido, la firma no coincide.
- **¿Qué guarda Angular y dónde?** El JWT solo en memoria; la cookie de sesión es `HttpOnly` (JavaScript no la puede leer). Se renueva antes de vencer usando la cookie.
- **¿Por qué dos cadenas de seguridad?** Para que cookie y JWT convivan sin mezclarse: con Bearer no se crea sesión (sin estado); sin Bearer todo funciona como antes.
- **¿Qué es OAuth2 y qué es OpenID Connect?** OAuth2 = permiso delegado; OpenID Connect = capa encima que además dice *quién* es la persona (ID token). Aquí usamos OIDC para identificarla.
- **¿Qué pasa si alguien roba el token?** Puede usarlo hasta que venza; por eso vive poco y en producción va con HTTPS.
- **¿Para qué sirve Docker aquí?** Que cualquiera levante MongoDB + backend + frontend con un solo comando y funcione igual en cualquier máquina o servidor.
