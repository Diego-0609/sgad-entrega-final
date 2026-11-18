# Publicar el SGAD en Railway

Railway da una URL pública con HTTPS (`https://algo.up.railway.app`), que es lo que Google exige para el login.
El sistema se publica como **3 servicios**: MongoDB, backend y frontend (nginx + Angular).

```
Internet ──https──> frontend (nginx)  ──red privada──> backend (Spring Boot) ──> MongoDB
```

Solo el frontend tiene dominio público. El backend y la base de datos quedan en la red privada de Railway.

## 0. Antes de empezar
1. Sube la carpeta del proyecto a un repositorio de GitHub (`.env` no se sube: ya está en `.gitignore`).
2. Crea la cuenta en https://railway.com e inicia sesión con GitHub.
3. Ten a mano el ID y el secreto del cliente de Google (Google Cloud → APIs y servicios → Credenciales).

## 1. Crear el proyecto y la base de datos
1. **New Project → Deploy MongoDB** (o *+ New → Database → Add MongoDB*). Deja el nombre `MongoDB`.

## 2. Servicio del backend
1. **+ New → GitHub Repo →** elige tu repositorio.
2. En el servicio: **Settings → Source → Root Directory** = `/sgad-backend`. Ponle de nombre `Backend`.
3. **Variables** (pestaña Variables → *Raw Editor*):

```
PORT=8080
SGAD_MONGO_URI=${{MongoDB.MONGO_URL}}/sgad?authSource=admin
SGAD_JWT_SECRET=<una frase larga y aleatoria, mínimo 32 caracteres>
SGAD_COOKIE_SECURE=true
SGAD_USUARIOS_DEMO=false
SGAD_ADMIN_PASSWORD=<la clave del administrador>
SGAD_GOOGLE_CLIENT_ID=<id de Google>
SGAD_GOOGLE_CLIENT_SECRET=<secreto de Google>
```

4. **No** generes dominio público para este servicio.

## 3. Servicio del frontend
1. **+ New → GitHub Repo →** el mismo repositorio.
2. **Settings → Source → Root Directory** = `/sgad-frontend`. Ponle de nombre `Frontend`.
3. **Variables:**

```
PORT=80
BACKEND_URL=http://${{Backend.RAILWAY_PRIVATE_DOMAIN}}:8080
NGINX_RESOLVER=[fd12::10]
```

4. **Settings → Networking → Generate Domain** (puerto **80**). Copia la URL que te da, por ejemplo
   `https://sgad-frontend-production.up.railway.app`.

## 4. Decirle al backend cuál es su dirección pública
En el servicio `Backend`, agrega la variable y deja que se vuelva a desplegar:

```
SGAD_FRONTEND_URL=https://sgad-frontend-production.up.railway.app
```

(sin `/` al final y con **la URL del frontend**, no la del backend).

## 5. Google Cloud
En el cliente OAuth de tipo *Aplicación web*, en **URIs de redireccionamiento autorizados**, agrega:

```
https://sgad-frontend-production.up.railway.app/login/oauth2/code/google
```

Mientras la app de Google esté en modo *Prueba*, solo entran los correos agregados como **usuarios de prueba**
(Pantalla de consentimiento de OAuth → Usuarios de prueba).

## 6. Probar
Abre la URL del frontend. Debe salir el login. Entra como `admin` con la clave que pusiste en `SGAD_ADMIN_PASSWORD`.

## Si algo falla
| Síntoma | Causa probable |
|---|---|
| El frontend responde **502** | `BACKEND_URL` mal escrito, el nombre del servicio no es `Backend`, o el backend aún está arrancando (espera 1-2 min) |
| El backend se reinicia solo | `SGAD_MONGO_URI` incorrecta (revisa `authSource=admin`) o `SGAD_JWT_SECRET` de menos de 32 caracteres |
| Google dice **redirect_uri_mismatch** | La URI de Google Cloud no es exactamente `https://<frontend>/login/oauth2/code/google` |
| Entras al login y te devuelve al login | Falta `SGAD_COOKIE_SECURE=true` o `SGAD_FRONTEND_URL` no coincide con la URL real |
| Google: *acceso bloqueado* | Tu correo no está en usuarios de prueba |

Ver los registros: en cada servicio, pestaña **Deployments → View logs**.

## Notas
- El primer despliegue tarda unos 5 minutos: el backend compila y corre sus pruebas unitarias antes de arrancar.
- Con `SGAD_USUARIOS_DEMO=false` solo existe el usuario `admin`. Entrenadores y deportistas se registran en **Crear cuenta** (o con Google).
- Cada `git push` a la rama conectada vuelve a desplegar el servicio.
