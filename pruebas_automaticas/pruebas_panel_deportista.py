"""
Pruebas de integración: panel del deportista, reportes, permisos por rol y registro con Google.

Uso:  python pruebas_panel_deportista.py [URL_DEL_SISTEMA]      (por defecto http://localhost)
Requiere: pip install requests. El sistema debe estar arriba con la base de datos VACÍA y las claves demo
(admin/admin123, entrenador/entrenador123, deportista/deportista123) y con SGAD_GOOGLE_CLIENT_ID definido.
"""
import re
import sys
from datetime import date, timedelta

import requests

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost").rstrip("/")
ok = fallos = 0


def check(nombre, condicion, detalle=""):
    global ok, fallos
    if condicion:
        ok += 1
        print(f"  OK   {nombre}")
    else:
        fallos += 1
        print(f"  FALLA {nombre} {detalle}")


def token(usuario, clave):
    r = requests.post(f"{BASE}/api/auth/token", json={"username": usuario, "password": clave})
    assert r.status_code == 200, f"No se pudo entrar como {usuario}: {r.status_code} {r.text}"
    return {"Authorization": f"Bearer {r.json()['accessToken']}"}


def api(metodo, ruta, h, **kw):
    return requests.request(metodo, f"{BASE}/api{ruta}", headers=h, **kw)


admin, entrenador, deportista = token("admin", "admin123"), token("entrenador", "entrenador123"), token("deportista", "deportista123")

print("\n== Permisos por rol")
for ruta in ("/deportistas", "/entrenamientos", "/asistencias", "/reportes", "/materiales"):
    check(f"DEPORTISTA no puede leer {ruta}", api("GET", ruta, deportista).status_code == 403)
    check(f"ENTRENADOR sí puede leer {ruta}", api("GET", ruta, entrenador).status_code == 200)
check("DEPORTISTA no puede enviar reportes", api("POST", "/reportes", deportista, json={}).status_code == 403)
check("ENTRENADOR no entra a /api/mi", api("GET", "/mi/perfil", entrenador).status_code == 403)
check("ADMIN no entra a /api/mi", api("GET", "/mi/perfil", admin).status_code == 403)
check("Sin sesión /api/mi responde 401", requests.get(f"{BASE}/api/mi/perfil").status_code == 401)

print("\n== Ficha del deportista")
perfil = api("GET", "/mi/perfil", deportista).json()
check("La ficha se crea sola la primera vez", perfil["deportista"]["idDeportista"] is not None)
check("Sin escuela todavía", perfil["escuela"] is None and perfil["entrenadores"] == [])
check("Segunda consulta devuelve la misma ficha",
      api("GET", "/mi/perfil", deportista).json()["deportista"]["idDeportista"] == perfil["deportista"]["idDeportista"])
mi_id = perfil["deportista"]["idDeportista"]

print("\n== Datos de prueba (escuela, entrenador, entrenamientos, asistencias)")
esc = api("POST", "/escuelas", admin, json={"nombre": "Tiburones", "direccion": "Calle 1", "telefono": "3000000000"}).json()
ent = api("POST", "/entrenadores", admin, json={"nombre": "Carlos Pérez", "especialidad": "Fútbol",
                                                 "telefono": "3111111111", "idEscuela": esc["idEscuela"]}).json()
r = api("PUT", f"/deportistas/{mi_id}", admin, json={"nombre": "Deportista Demo", "edad": 17, "genero": "M",
                                                      "deporte": "Fútbol", "idEscuela": esc["idEscuela"]})
check("El personal completa la ficha del deportista", r.status_code == 200, r.text)

hoy = date.today()
fechas = {"hace3": hoy - timedelta(days=3), "hace5": hoy - timedelta(days=5), "hace8": hoy - timedelta(days=8),
          "futuro": hoy + timedelta(days=2)}
ids = {}
for clave, f in fechas.items():
    t = api("POST", "/entrenamientos", admin, json={"fecha": f.isoformat(), "hora": "16:30", "lugar": f"Cancha {clave}",
                                                    "idEntrenador": ent["idEntrenador"]})
    ids[clave] = t.json()["idEntrenamiento"]
for clave, estado in (("hace3", "presente"), ("hace5", "presente"), ("hace8", "ausente")):
    api("POST", "/asistencias", admin, json={"estado": estado, "idDeportista": mi_id, "idEntrenamiento": ids[clave]})

print("\n== Panel del deportista")
perfil = api("GET", "/mi/perfil", deportista).json()
check("Perfil muestra su escuela y su entrenador", perfil["escuela"] == "Tiburones"
      and perfil["entrenadores"][0]["nombre"] == "Carlos Pérez")
entrenamientos = api("GET", "/mi/entrenamientos", deportista).json()
check("Ve los 4 entrenamientos de su escuela", len(entrenamientos) == 4, str(len(entrenamientos)))
check("Vienen ordenados del más reciente al más antiguo",
      [e["fecha"] for e in entrenamientos] == sorted([e["fecha"] for e in entrenamientos], reverse=True))
check("Cada uno trae fecha, hora, lugar y entrenador",
      all(e["hora"] and e["lugar"] and e["entrenador"] == "Carlos Pérez" for e in entrenamientos))
asist = api("GET", "/mi/asistencias", deportista).json()
check("Mi asistencia: solo los 3 con registro", len(asist) == 3 and {a["asistencia"] for a in asist} == {"presente", "ausente"})
est = api("GET", "/mi/estadisticas", deportista).json()
res = est["resumen"]
check("Estadísticas: 3 registros, 2 presentes, 1 ausente, 67%",
      (res["total"], res["presentes"], res["ausentes"], res["porcentaje"]) == (3, 2, 1, 67), str(res))
check("Racha de 2 presentes seguidas", res["racha"] == 2)
check("Trae 4 semanas de historial", len(res["ultimasSemanas"]) == 4)
check("El próximo entrenamiento es el de dentro de 2 días", est["proximo"] and est["proximo"]["fecha"] == fechas["futuro"].isoformat())

print("\n== Reportes")
r = api("POST", "/reportes", entrenador, json={"idDeportista": mi_id, "titulo": "Buena semana",
                                              "mensaje": "Mejoró la resistencia.", "calificacion": 5})
check("ENTRENADOR envía un reporte (201)", r.status_code == 201, r.text)
rep = r.json()
check("El reporte guarda al autor y queda sin leer", rep["autor"] == "entrenador" and rep["leido"] is False)
check("Validación: título y mensaje obligatorios",
      api("POST", "/reportes", entrenador, json={"idDeportista": mi_id, "titulo": "", "mensaje": ""}).status_code == 400)
check("Validación: calificación de 1 a 5",
      api("POST", "/reportes", entrenador, json={"idDeportista": mi_id, "titulo": "x", "mensaje": "y", "calificacion": 9}).status_code == 400)
check("Deportista inexistente responde 404",
      api("POST", "/reportes", entrenador, json={"idDeportista": "no-existe", "titulo": "x", "mensaje": "y"}).status_code == 404)
check("El buzón del deportista tiene el reporte", len(api("GET", "/mi/reportes", deportista).json()) == 1)
check("El perfil cuenta 1 reporte sin leer", api("GET", "/mi/perfil", deportista).json()["reportesNoLeidos"] == 1)

s = requests.Session()
html = s.get(f"{BASE}/registro").text
csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', html)
datos = {"username": "otro_dep", "password": "clave1234", "confirmarPassword": "clave1234", "rol": "ROLE_DEPORTISTA"}
if csrf:
    datos["_csrf"] = csrf.group(1)
r = s.post(f"{BASE}/registro", data=datos, allow_redirects=False)
check("Registro local como deportista redirige al login", r.status_code == 302 and "registrado=true" in r.headers.get("Location", ""))
otro_h = token("otro_dep", "clave1234")
check("El nuevo deportista recibe su ficha automáticamente",
      api("GET", "/mi/perfil", otro_h).json()["deportista"]["nombre"] == "otro_dep")
check("Otro deportista NO ve mis reportes", api("GET", "/mi/reportes", otro_h).json() == [])
check("Otro deportista NO puede marcar mi reporte como leído",
      api("PUT", f"/mi/reportes/{rep['idReporte']}/leido", otro_h).status_code == 404)
r = api("PUT", f"/mi/reportes/{rep['idReporte']}/leido", deportista)
check("El dueño lo marca como leído", r.status_code == 200 and r.json()["leido"] is True)
check("Ya no hay reportes sin leer", api("GET", "/mi/perfil", deportista).json()["reportesNoLeidos"] == 0)
check("El personal ve el reporte como leído", api("GET", "/reportes", entrenador).json()[0]["leido"] is True)
check("Otro deportista no ve mis entrenamientos (sin escuela)", api("GET", "/mi/entrenamientos", otro_h).json() == [])
check("ENTRENADOR elimina un reporte", api("DELETE", f"/reportes/{rep['idReporte']}", entrenador).status_code == 204)

print("\n== Login y registro (páginas)")
login = requests.get(f"{BASE}/login").text
check("El login ya no muestra la tarjeta de Administrador", "Administrador" not in login)
check("El login conserva Entrenador y Deportista", "Entrenador" in login and "Deportista" in login)
check("El login tiene «Continuar con Google»", "Continuar con Google" in login)
reg = requests.get(f"{BASE}/registro").text
check("El registro no ofrece el rol admin", "ROLE_ADMIN" not in reg)
check("El registro tiene «Registrarme con Google»", "Registrarme con Google" in reg)
check("Aviso cuando un correo de Google no tiene cuenta",
      "todavía no tiene cuenta" in requests.get(f"{BASE}/registro?sinCuenta=true").text)

print("\n== Registro con Google (se detiene en la pantalla de Google)")
r = requests.get(f"{BASE}/registro/google?rol=ROLE_ADMIN", allow_redirects=False)
check("Rol admin por Google se rechaza y vuelve al registro", r.status_code == 302 and r.headers["Location"].endswith("/registro"))
r = requests.get(f"{BASE}/registro/google?rol=ROLE_INVENTADO", allow_redirects=False)
check("Rol inventado se rechaza", r.status_code == 302 and r.headers["Location"].endswith("/registro"))
sesion = requests.Session()
r = sesion.get(f"{BASE}/registro/google?rol=ROLE_ENTRENADOR", allow_redirects=False)
check("Rol entrenador: va al flujo de Google", r.status_code == 302 and "/oauth2/authorization/google" in r.headers["Location"])
r = sesion.get(f"{BASE}/oauth2/authorization/google", allow_redirects=False)
check("Spring manda a accounts.google.com con el cliente configurado",
      r.status_code == 302 and "accounts.google.com" in r.headers["Location"] and "client_id=" in r.headers["Location"])
check("Se abre una sesión para recordar el rol elegido", "JSESSIONID" in sesion.cookies.get_dict())

print(f"\nResultado: {ok} OK, {fallos} con falla")
sys.exit(1 if fallos else 0)
