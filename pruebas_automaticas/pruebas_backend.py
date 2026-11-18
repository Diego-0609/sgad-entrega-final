import re, sys, time, json, requests

BASE = "http://localhost:8080"
ORIGIN = {"Origin": "http://localhost:4200"}
resultados = []
NUEVO = "nuevo" + str(int(time.time()))[-6:]

def check(nombre, condicion, detalle=""):
    resultados.append((nombre, bool(condicion), detalle))
    print(("PASS " if condicion else "FAIL ") + nombre + ("" if condicion else f"   -> {detalle}"), flush=True)

def login(user, pwd):
    s = requests.Session()
    r = s.get(BASE + "/login")
    token = re.search(r'name="_csrf"\s+value="([^"]+)"', r.text)
    data = {"username": user, "password": pwd}
    if token: data["_csrf"] = token.group(1)
    r = s.post(BASE + "/login", data=data, allow_redirects=False)
    return s, r

def j(r):
    try: return r.json()
    except Exception: return {}

# ───────── 1. Autenticación ─────────
r = requests.get(BASE + "/api/escuelas", headers=ORIGIN)
check("API sin sesión -> 401 JSON NO_AUTENTICADO", r.status_code == 401 and j(r).get("codigo") == "NO_AUTENTICADO", r.text[:200])
check("401 incluye cabecera CORS para Angular", r.headers.get("Access-Control-Allow-Origin") == "http://localhost:4200", dict(r.headers))

s, r = login("admin", "mala")
check("Login con contraseña incorrecta -> /login?error", r.status_code == 302 and "error" in r.headers.get("Location", ""), r.headers.get("Location"))

admin, r = login("admin", "admin123")
check("Login admin correcto -> redirige al frontend", r.status_code == 302 and r.headers["Location"].startswith("http://localhost:4200"), r.headers.get("Location"))
me = admin.get(BASE + "/api/auth/me", headers=ORIGIN)
check("/api/auth/me devuelve rol y duración de sesión", me.status_code == 200 and j(me).get("rol") == "ROLE_ADMIN" and j(me).get("sessionTimeoutSeconds") == 60, me.text)

# ───────── 2. CRUD completo como ADMIN ─────────
r = admin.post(BASE + "/api/escuelas", json={"nombre": "Escuela Test", "direccion": "Calle 1", "telefono": "3001234567"})
check("ADMIN crea escuela -> 201", r.status_code == 201 and j(r).get("idEscuela"), r.text)
esc = j(r).get("idEscuela")
r = admin.get(BASE + "/api/escuelas")
check("ADMIN lista escuelas", r.status_code == 200 and any(e["idEscuela"] == esc for e in r.json()))
r = admin.put(f"{BASE}/api/escuelas/{esc}", json={"nombre": "Escuela Test 2", "direccion": "Calle 2", "telefono": "3001234567"})
check("ADMIN edita escuela", r.status_code == 200 and j(r).get("nombre") == "Escuela Test 2", r.text)

r = admin.post(BASE + "/api/entrenadores", json={"nombre": "Carlos", "especialidad": "Fútbol", "telefono": "3109998888", "idEscuela": esc})
check("ADMIN crea entrenador", r.status_code == 201, r.text); ent = j(r).get("idEntrenador")
r = admin.post(BASE + "/api/deportistas", json={"nombre": "Ana", "edad": 15, "genero": "Femenino", "deporte": "Fútbol", "idEscuela": esc})
check("ADMIN crea deportista", r.status_code == 201, r.text); dep = j(r).get("idDeportista")
r = admin.post(BASE + "/api/entrenamientos", json={"fecha": "2026-11-01", "hora": "08:30", "lugar": "Cancha 1", "idEntrenador": ent})
check("ADMIN crea entrenamiento (fecha/hora)", r.status_code == 201, r.text); tra = j(r).get("idEntrenamiento")
r = admin.post(BASE + "/api/asistencias", json={"estado": "presente", "idDeportista": dep, "idEntrenamiento": tra})
check("ADMIN crea asistencia", r.status_code == 201, r.text); asi = j(r).get("idAsistencia")
r = admin.post(BASE + "/api/materiales", json={"nombre": "Balones", "cantidad": 10, "estado": "bueno", "idEscuela": esc})
check("ADMIN crea material (embebido)", r.status_code == 201 and j(r).get("idMaterial"), r.text); mat = j(r).get("idMaterial")
r = admin.put(f"{BASE}/api/materiales/{mat}", json={"nombre": "Balones", "cantidad": 12, "estado": "regular", "idEscuela": esc})
check("ADMIN edita material", r.status_code == 200 and j(r).get("cantidad") == 12, r.text)
r = admin.put(f"{BASE}/api/asistencias/{asi}", json={"estado": "ausente", "idDeportista": dep, "idEntrenamiento": tra})
check("ADMIN edita asistencia", r.status_code == 200 and j(r).get("estado") == "ausente", r.text)
r = admin.put(f"{BASE}/api/deportistas/{dep}", json={"nombre": "Ana María", "edad": 16, "genero": "Femenino", "deporte": "Fútbol", "idEscuela": esc})
check("ADMIN edita deportista", r.status_code == 200 and j(r).get("edad") == 16, r.text)

# ───────── 3. Control de errores ─────────
r = admin.post(BASE + "/api/escuelas", json={})
check("Validación: escuela sin nombre -> 400 VALIDACION con detalle por campo", r.status_code == 400 and j(r).get("codigo") == "VALIDACION" and "nombre" in j(r).get("errores", {}), r.text)
r = admin.post(BASE + "/api/deportistas", json={"nombre": "X", "edad": "abc", "genero": "M", "deporte": "F", "idEscuela": esc})
check("Tipo incorrecto (edad='abc') -> 400 JSON_INVALIDO, sin 500", r.status_code == 400 and j(r).get("codigo") == "JSON_INVALIDO", r.text)
r = admin.post(BASE + "/api/deportistas", json={"nombre": "X", "edad": 0, "genero": "M", "deporte": "F", "idEscuela": esc})
check("Validación: edad 0 -> 400", r.status_code == 400 and "edad" in j(r).get("errores", {}), r.text)
r = admin.post(BASE + "/api/deportistas", json={"nombre": "X", "edad": 10, "genero": "M", "deporte": "F", "idEscuela": "no-existe"})
check("Referencia a escuela inexistente -> 400 (antes 500)", r.status_code == 400 and j(r).get("codigo") == "SOLICITUD_INVALIDA", r.text)
r = admin.post(BASE + "/api/deportistas", json={"nombre": "X", "edad": 10, "genero": "M", "deporte": "F"})
check("Falta idEscuela -> 400 claro", r.status_code == 400 and "idEscuela" in j(r).get("mensaje", ""), r.text)
r = admin.post(BASE + "/api/entrenamientos", json={"fecha": "no-es-fecha", "hora": "08:30", "lugar": "L", "idEntrenador": ent})
check("Fecha mal formada -> 400 JSON_INVALIDO", r.status_code == 400 and j(r).get("codigo") == "JSON_INVALIDO", r.text)
r = admin.post(BASE + "/api/asistencias", json={"estado": "tal vez", "idDeportista": dep, "idEntrenamiento": tra})
check("Estado de asistencia inválido -> 400", r.status_code == 400 and "estado" in j(r).get("errores", {}), r.text)
r = admin.put(f"{BASE}/api/escuelas/id-inexistente", json={"nombre": "Z"})
check("PUT a id inexistente -> 404 NO_ENCONTRADO", r.status_code == 404 and j(r).get("codigo") == "NO_ENCONTRADO", r.text)
r = admin.delete(f"{BASE}/api/deportistas/id-inexistente")
check("DELETE a id inexistente -> 404", r.status_code == 404, r.text)
r = admin.get(f"{BASE}/api/escuelas/id-inexistente")
check("GET a id inexistente -> 404 JSON", r.status_code == 404 and j(r).get("codigo") == "NO_ENCONTRADO", r.text)
r = admin.post(BASE + "/api/escuelas", data="esto no es json", headers={"Content-Type": "application/json"})
check("JSON roto -> 400 (no 500)", r.status_code == 400, r.text)
r = admin.get(BASE + "/api/ruta-que-no-existe")
check("GET a ruta de API inexistente -> 404 con el mismo formato JSON de error", r.status_code == 404 and j(r).get("codigo") == "NOT_FOUND" and "mensaje" in j(r), f"{r.status_code} {r.text[:150]}")
r = admin.post(BASE + "/api/ruta-que-no-existe", json={})
check("POST a ruta de API no prevista -> bloqueada (403 JSON)", r.status_code == 403 and j(r).get("codigo") == "ACCESO_DENEGADO", f"{r.status_code} {r.text[:150]}")
check("Errores no filtran detalles internos", "Exception" not in r.text and "at com." not in r.text)

# ───────── 4. Autorización por rol ─────────
dpt, _ = login("deportista", "deportista123")
check("DEPORTISTA puede leer escuelas", dpt.get("http://localhost:8080/api/escuelas").status_code == 200)
for nombre, metodo, ruta, cuerpo in [
    ("DEPORTISTA NO puede crear escuela", "post", "/api/escuelas", {"nombre": "Hack"}),
    ("DEPORTISTA NO puede borrar escuela", "delete", f"/api/escuelas/{esc}", None),
    ("DEPORTISTA NO puede crear entrenador", "post", "/api/entrenadores", {"nombre": "H", "especialidad": "x", "idEscuela": esc}),
    ("DEPORTISTA NO puede crear deportista", "post", "/api/deportistas", {"nombre": "H", "edad": 10, "genero": "M", "deporte": "x", "idEscuela": esc}),
    ("DEPORTISTA NO puede registrar asistencia", "post", "/api/asistencias", {"estado": "presente", "idDeportista": dep, "idEntrenamiento": tra}),
    ("DEPORTISTA NO puede editar entrenamientos", "put", f"/api/entrenamientos/{tra}", {"fecha": "2026-11-01", "hora": "08:30", "lugar": "L"}),
    ("DEPORTISTA NO puede ver materiales", "get", "/api/materiales", None),
]:
    r = getattr(dpt, metodo)(BASE + ruta, json=cuerpo) if cuerpo is not None else getattr(dpt, metodo)(BASE + ruta)
    check(nombre + " -> 403", r.status_code == 403 and j(r).get("codigo") == "ACCESO_DENEGADO", f"{r.status_code} {r.text[:120]}")
r = admin.get(f"{BASE}/api/escuelas/{esc}")
check("La escuela sigue existiendo tras el intento de borrado del deportista", r.status_code == 200)

ent_s, _ = login("entrenador", "entrenador123")
r = ent_s.post(BASE + "/api/deportistas", json={"nombre": "Luis", "edad": 14, "genero": "Masculino", "deporte": "Natación", "idEscuela": esc})
check("ENTRENADOR crea deportista -> 201", r.status_code == 201, r.text); dep2 = j(r).get("idDeportista")
check("ENTRENADOR lee materiales", ent_s.get(BASE + "/api/materiales").status_code == 200)
check("ENTRENADOR NO puede crear escuela", ent_s.post(BASE + "/api/escuelas", json={"nombre": "X"}).status_code == 403)
check("ENTRENADOR NO puede borrar entrenadores", ent_s.delete(f"{BASE}/api/entrenadores/{ent}").status_code == 403)
check("ENTRENADOR borra deportista", ent_s.delete(f"{BASE}/api/deportistas/{dep2}").status_code == 204)

# páginas del backend
r = dpt.get(BASE + "/admin/zona", allow_redirects=False)
check("DEPORTISTA en /admin/zona -> redirige a /403", r.status_code == 302 and r.headers["Location"].endswith("/403"), f"{r.status_code} {r.headers.get('Location')}")
r = dpt.get(BASE + "/admin/zona")
check("...y la página 403 se muestra", r.status_code == 403 or "No tienes permisos" in r.text, r.status_code)
r = ent_s.get(BASE + "/admin/zona", allow_redirects=False)
check("ENTRENADOR en /admin/zona -> /403", r.status_code == 302 and r.headers["Location"].endswith("/403"))
check("ENTRENADOR entra a /entrenador/zona", ent_s.get(BASE + "/entrenador/zona").status_code == 200)
check("ADMIN entra a /admin/zona", admin.get(BASE + "/admin/zona").status_code == 200)
r = requests.get(BASE + "/panel", allow_redirects=False, headers={"Accept": "text/html"})
check("/panel sin sesión (navegador) -> redirige a /login", r.status_code == 302 and "/login" in r.headers["Location"], f"{r.status_code} {r.headers.get('Location')}")
r = requests.get(BASE + "/panel", allow_redirects=False)
check("/panel sin sesión (cliente sin Accept html) también -> /login", r.status_code == 302 and "/login" in r.headers.get("Location", ""), f"{r.status_code} {r.headers.get('Location')}")
check("/publico es público", requests.get(BASE + "/publico").status_code == 200)

# ───────── 5. Registro ─────────
def registrar(user, pwd, conf, rol):
    s = requests.Session()
    r = s.get(BASE + "/registro")
    tok = re.search(r'name="_csrf"\s+value="([^"]+)"', r.text).group(1)
    return s.post(BASE + "/registro", data={"username": user, "password": pwd, "confirmarPassword": conf, "rol": rol, "_csrf": tok}, allow_redirects=False)

r = registrar("ab", "clave123", "clave123", "ROLE_DEPORTISTA")
check("Registro: usuario muy corto rechazado", r.status_code == 200 and "entre 3 y 30" in r.text)
r = registrar(NUEVO, "123", "123", "ROLE_DEPORTISTA")
check("Registro: contraseña corta rechazada en el servidor", r.status_code == 200 and "entre 6 y 72" in r.text)
r = registrar(NUEVO, "clave123", "otra", "ROLE_DEPORTISTA")
check("Registro: contraseñas distintas rechazadas", r.status_code == 200 and "no coinciden" in r.text)
r = registrar(NUEVO, "clave123", "clave123", "ROLE_ADMIN")
check("Registro: NO permite autoregistrarse como ADMIN", r.status_code == 200 and "Rol no válido" in r.text)
r = registrar(NUEVO, "clave123", "clave123", "ROLE_DEPORTISTA")
check("Registro: usuario válido -> /login?registrado", r.status_code == 302 and "registrado" in r.headers.get("Location", ""), f"{r.status_code} {r.text[:200]}")
r = registrar(NUEVO, "clave123", "clave123", "ROLE_ENTRENADOR")
check("Registro: usuario duplicado rechazado", r.status_code == 200 and "ya está en uso" in r.text)
r = registrar("admin", "clave123", "clave123", "ROLE_ENTRENADOR")
check("Registro: no se puede suplantar 'admin'", r.status_code == 200 and "ya está en uso" in r.text)
nuevo, r = login(NUEVO, "clave123")
check("El usuario recién registrado puede iniciar sesión", r.status_code == 302 and r.headers["Location"].startswith("http://localhost:4200"), r.headers.get("Location"))
check("...y tiene rol DEPORTISTA", j(nuevo.get(BASE + "/api/auth/me")).get("rol") == "ROLE_DEPORTISTA")

# ───────── 6. Logout (GET y POST) ─────────
r = ent_s.get(BASE + "/logout", allow_redirects=False)
check("Logout por GET -> /login?logout", r.status_code == 302 and "logout" in r.headers["Location"], r.headers.get("Location"))
check("Tras el logout la API responde 401", ent_s.get(BASE + "/api/escuelas").status_code == 401)
panel = admin.get(BASE + "/panel")
tok = re.search(r'name="_csrf"\s+value="([^"]+)"', panel.text)
r = admin.post(BASE + "/logout", data={"_csrf": tok.group(1)} if tok else {}, allow_redirects=False)
check("Botón 'Cerrar sesión' del panel (POST /logout) funciona", r.status_code == 302 and "logout" in r.headers.get("Location", ""), f"{r.status_code} {r.headers.get('Location')}")
check("Tras el logout POST la sesión murió", admin.get(BASE + "/api/escuelas").status_code == 401)

# ───────── 7. Página 404 ─────────
a2, _ = login("admin", "admin123")
r = a2.get(BASE + "/pagina-que-no-existe", headers={"Accept": "text/html"})
check("Página inexistente (navegador) muestra la 404 propia", r.status_code == 404 and "no existe o fue movida" in r.text, f"{r.status_code} {r.text[:120]}")

# ───────── 8. Sesiones simultáneas ─────────
a3, _ = login("admin", "admin123")     # esto cierra la sesión a2 (límite = 1)
r = a2.get(BASE + "/api/escuelas", headers=ORIGIN)
check("Sesiones simultáneas: la sesión antigua queda cerrada (SESION_REEMPLAZADA)", r.status_code == 401 and j(r).get("codigo") == "SESION_REEMPLAZADA", f"{r.status_code} {r.text[:150]}")
check("...y la nueva sigue activa", a3.get(BASE + "/api/escuelas").status_code == 200)

# limpieza de datos de prueba
admin2, _ = login("admin", "admin123")
for ruta in [f"/api/asistencias/{asi}", f"/api/entrenamientos/{tra}", f"/api/deportistas/{dep}", f"/api/materiales/{mat}", f"/api/entrenadores/{ent}", f"/api/escuelas/{esc}"]:
    admin2.delete(BASE + ruta)

import os
if os.environ.get("SOLO_RAPIDAS"):
    total = len(resultados); ok = sum(1 for _, p, _ in resultados if p)
    print(f"\nRESULTADO (rápidas): {ok}/{total} pruebas OK"); sys.exit(0 if ok == total else 1)
# ───────── 9. Expiración de sesión (timeout = 1 minuto) ─────────
print("\n-- Prueba de expiración: timeout configurado en 1 minuto --", flush=True)
exp, _ = login("deportista", "deportista123")
t0 = time.time()
time.sleep(40)
r = exp.get(BASE + "/api/escuelas", headers=ORIGIN)
check("t=40s: la sesión sigue viva", r.status_code == 200)
time.sleep(40)   # 80s desde el login, pero solo 40s desde la última actividad
r = exp.get(BASE + "/api/escuelas", headers=ORIGIN)
check("t=80s: sigue viva porque el timeout se RENUEVA con la actividad (40s de inactividad < 60s)", r.status_code == 200, r.status_code)
time.sleep(70)   # >60s sin actividad
r = exp.get(BASE + "/api/escuelas", headers=ORIGIN)
check("Tras >60s de inactividad -> 401 SESION_EXPIRADA (JSON)", r.status_code == 401 and j(r).get("codigo") == "SESION_EXPIRADA", f"{r.status_code} {r.text[:150]}")
check("...con cabecera CORS para que Angular lo pueda leer", r.headers.get("Access-Control-Allow-Origin") == "http://localhost:4200")
# página web con cookie vencida (otra sesión vencida)
exp2, _ = login("entrenador", "entrenador123")
time.sleep(65)
r = exp2.get(BASE + "/panel", allow_redirects=False)
check("Web con sesión vencida -> /login?expired=true", r.status_code == 302 and "expired=true" in r.headers.get("Location", ""), f"{r.status_code} {r.headers.get('Location')}")
r = exp2.get(BASE + "/login")
check("...y /login se muestra sin bucle de redirecciones, con el aviso de sesión expirada", r.status_code == 200)
r = exp2.get(BASE + "/login?expired=true")
check("Mensaje 'Tu sesión expiró por inactividad' visible", "expiró por inactividad" in r.text)

total = len(resultados); ok = sum(1 for _, p, _ in resultados if p)
print(f"\nRESULTADO: {ok}/{total} pruebas OK")
sys.exit(0 if ok == total else 1)
