# Cambios v3 (entrega final)

## 1. Login con Google por rol
- **Registro** (`/registro`): botón «Registrarme con Google». Se elige Entrenador o Deportista y Google crea la cuenta con ese rol.
- **Login** (`/login`): «Continuar con Google» entra con el rol que la cuenta ya tiene en MongoDB.
- Un correo de Google sin cuenta vuelve al registro con un aviso. El rol administrador nunca se puede crear por Google.
- Código: `security/GoogleOidcUserService.java`, `controller/RegistroController.java`, `config/SecurityConfig.java`.

## 2. Administrador fuera del frontend
- El login ya no muestra la tarjeta de administrador ni el registro ofrece ese rol. El admin existe solo en la base de datos.

## 3. Panel del deportista (`/mi-panel`)
| Pantalla | Qué muestra |
|---|---|
| Mi panel | próximo entrenamiento, % de asistencia, semana actual, reportes sin leer, sus entrenadores |
| Entrenamientos | día, hora, lugar y entrenador |
| Asistencia | presente/ausente por fecha, hora y entrenador |
| Estadísticas | porcentaje, racha y las últimas 4 semanas |
| Buzón | reportes que le envían los entrenadores (con contador sin leer) |
- Backend: `controller/MiController.java`, `service/MiPanelService.java`, `service/EstadisticasService.java`. Todo sale filtrado por el propio deportista (`/api/mi/**`).
- Frontend: `components/mi-panel`, `mis-entrenamientos`, `mi-asistencia`, `mis-estadisticas`, `buzon`.

## 4. Reportes de entrenadores
- Entrenador o admin: pantalla **Reportes** (`/reportes`) para enviar un reporte a un deportista (título, mensaje, calificación 1–5).
- Código: `entity/Reporte.java`, `service/ReporteService.java`, `controller/ReporteController.java`.

## 5. Permisos más estrictos
- El deportista ya no puede leer deportistas, entrenamientos, asistencias, materiales ni reportes del personal; solo `/api/mi/**`.

## 6. Railway
- `README_RAILWAY.md`: guía paso a paso. El backend lee `PORT` y el nginx usa variables (`PORT`, `BACKEND_URL`, `NGINX_RESOLVER`).

## Pruebas
- `mvn test` (incluye `EstadisticasServiceTest` y `GoogleOidcUserServiceTest`).
- `pruebas_automaticas/pruebas_panel_deportista.py` (54 comprobaciones) y la suite anterior (75).
