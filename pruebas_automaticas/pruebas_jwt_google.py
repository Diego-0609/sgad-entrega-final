"""
Pruebas de JWT (convive con la sesión/cookie) y del botón/flujo de Google (OAuth2).

Uso:
  Backend normal en :8080  (sin credenciales de Google)      -> parte A y B
  Backend con SGAD_GOOGLE_CLIENT_ID=... en :8081             -> parte C
  python pruebas_jwt_google.py            (requiere: pip install requests pymongo)
"""
import re, sys, time, json, hmac, hashlib, base64, requests
from urllib.parse import urlparse, parse_qs

BASE = "http://localhost:8080"
BASE_GOOGLE = "http://localhost:8081"
ORIGIN = {"Origin": "http://localhost:4200"}
CLAVE_DEV = "sgad-clave-SOLO-para-desarrollo-cambiala-en-produccion-123456"
resultados = []
NUEVO = "jwt" + str(int(time.time()))[-6:]

def check(nombre, condicion, detalle=""):
    resultados.append((nombre, bool(condicion), detalle))
    print(("PASS " if condicion else "FAIL ") + nombre + ("" if condicion else f"   -> {detalle}"), flush=True)

def j(r):
    try: return r.json()
    except Exception: return {}

def b64(b): return base64.urlsafe_b64encode(b).rstrip(b"=").decode()
def b64d(s): return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))

def token_manual(payload, clave=CLAVE_DEV, alg="HS256"):
    cab = b64(json.dumps({"alg": alg}).encode()); cont = b64(json.dumps(payload).encode())
    if alg == "none": return f"{cab}.{cont}."
    firma = hmac.new(clave.encode(), f"{cab}.{cont}".encode(), hashlib.sha256).digest()
    return f"{cab}.{cont}.{b64(firma)}"

def obtener_token(user, pwd):
    r = requests.post(BASE + "/api/auth/token", json={"username": user, "password": pwd})
    return r, j(r).get("accessToken")

def bearer(t): return {"Authorization": f"Bearer {t}"}

def login_sesion(user, pwd):
    s = requests.Session()
    r = s.get(BASE + "/login")
    csrf = re.search(r'name="_csrf"\s+value="([^"]+)"', r.text)
    data = {"username": user, "password": pwd}
    if csrf: data["_csrf"] = csrf.group(1)
    return s, s.post(BASE + "/login", data=data, allow_redirects=False)

# ═════════ A. Obtener el token ═════════
print("── A. Obtener el JWT ──")
r = requests.post(BASE + "/api/auth/token", json={"username": "admin", "password": "mala"})
check("Clave incorrecta -> 401 CREDENCIALES_INVALIDAS", r.status_code == 401 and j(r).get("codigo") == "CREDENCIALES_INVALIDAS", r.text)
r2 = requests.post(BASE + "/api/auth/token", json={"username": "no_existe_xx", "password": "x"})
check("Usuario inexistente da la MISMA respuesta (no revela qué usuarios existen)", r2.status_code == 401 and j(r2).get("mensaje") == j(r).get("mensaje"))
r = requests.post(BASE + "/api/auth/token", json={"username": "", "password": ""})
check("Campos vacíos -> 400", r.status_code == 400, r.text)

r, tk_admin = obtener_token("admin", "admin123")
cuerpo = j(r)
check("admin/admin123 -> 200 con accessToken Bearer", r.status_code == 200 and tk_admin and cuerpo.get("tokenType") == "Bearer" and cuerpo.get("expiresIn") == 3600, r.text)
check("El login por token NO crea sesión ni cookie", "JSESSIONID" not in r.headers.get("Set-Cookie", "") and not r.cookies, dict(r.headers))
partes = tk_admin.split(".")
payload = json.loads(b64d(partes[1]))
check("El token tiene 3 partes y su contenido trae usuario, rol y vencimiento",
      len(partes) == 3 and payload.get("sub") == "admin" and payload.get("rol") == "ROLE_ADMIN" and payload["exp"] - payload["iat"] == 3600, payload)
check("El token NO contiene la contraseña", "admin123" not in b64d(partes[1]).decode() and "$2a$" not in b64d(partes[1]).decode())

_, tk_ent = obtener_token("entrenador", "entrenador123")
_, tk_dep = obtener_token("deportista", "deportista123")

# ═════════ B. Usar el token ═════════
print("── B. Usar el JWT en la API ──")
r = requests.get(BASE + "/api/escuelas")
check("Sin token ni cookie -> 401 NO_AUTENTICADO", r.status_code == 401 and j(r).get("codigo") == "NO_AUTENTICADO", r.text)

r = requests.get(BASE + "/api/escuelas", headers=bearer(tk_admin))
check("GET con Bearer -> 200", r.status_code == 200, r.text[:200])
check("...y sin crear sesión (stateless: no hay Set-Cookie)", "JSESSIONID" not in r.headers.get("Set-Cookie", ""), r.headers.get("Set-Cookie"))

me = requests.get(BASE + "/api/auth/me", headers=bearer(tk_admin))
check("/api/auth/me con JWT -> metodo JWT, usuario y rol", me.status_code == 200 and j(me).get("metodo") == "JWT" and j(me).get("username") == "admin" and j(me).get("rol") == "ROLE_ADMIN" and j(me).get("tokenExpiraEn"), me.text)

H = bearer(tk_admin)
r = requests.post(BASE + "/api/escuelas", json={"nombre": "Escuela JWT", "direccion": "Calle 9", "telefono": "3001112233"}, headers=H)
check("ADMIN crea escuela con JWT (CRUD) -> 201", r.status_code == 201 and j(r).get("idEscuela"), r.text); esc = j(r).get("idEscuela")
r = requests.put(f"{BASE}/api/escuelas/{esc}", json={"nombre": "Escuela JWT 2", "direccion": "Calle 9", "telefono": "3001112233"}, headers=H)
check("ADMIN edita escuela con JWT", r.status_code == 200 and j(r).get("nombre") == "Escuela JWT 2", r.text)
r = requests.post(BASE + "/api/escuelas", json={"nombre": "", "direccion": "", "telefono": ""}, headers=H)
check("Las validaciones del backend también aplican con JWT -> 400", r.status_code == 400, r.text)
r = requests.delete(f"{BASE}/api/escuelas/{esc}", headers=H)
check("ADMIN borra escuela con JWT", r.status_code in (200, 204), r.text)

# Los roles se respetan igual que con la cookie
r = requests.get(BASE + "/api/escuelas", headers=bearer(tk_dep))
check("DEPORTISTA con JWT puede LEER escuelas", r.status_code == 200)
r = requests.post(BASE + "/api/escuelas", json={"nombre": "X", "direccion": "Y", "telefono": "3001112233"}, headers=bearer(tk_dep))
check("DEPORTISTA con JWT NO puede crear escuelas -> 403 ACCESO_DENEGADO", r.status_code == 403 and j(r).get("codigo") == "ACCESO_DENEGADO", r.text)
r = requests.get(BASE + "/api/materiales", headers=bearer(tk_dep))
check("DEPORTISTA con JWT NO ve materiales -> 403", r.status_code == 403)
r = requests.get(BASE + "/api/materiales", headers=bearer(tk_ent))
check("ENTRENADOR con JWT SÍ ve materiales", r.status_code == 200)
r = requests.post(BASE + "/api/escuelas", json={"nombre": "X", "direccion": "Y", "telefono": "3001112233"}, headers=bearer(tk_ent))
check("ENTRENADOR con JWT NO puede crear escuelas -> 403", r.status_code == 403)

# Ataques / tokens malos
print("── B2. Tokens inválidos ──")
r = requests.get(BASE + "/api/escuelas", headers=bearer("esto.no.es-un-token"))
check("Token basura -> 401 TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO", r.text)
alterado = partes[0] + "." + b64(json.dumps({**payload, "sub": "deportista", "rol": "ROLE_ADMIN"}).encode()) + "." + partes[2]
r = requests.get(BASE + "/api/escuelas", headers=bearer(alterado))
check("Token con el contenido alterado (firma no coincide) -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO", r.text)
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "sgad-backend", "sub": "admin", "rol": "ROLE_ADMIN", "exp": int(time.time()) + 600}, clave="una-clave-que-el-atacante-se-invento-123456789")))
check("Token firmado con OTRA clave -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO")
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "sgad-backend", "sub": "admin", "rol": "ROLE_ADMIN", "exp": int(time.time()) + 600}, alg="none")))
check("Token sin firma (alg=none) -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO")
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "sgad-backend", "sub": "admin", "rol": "ROLE_ADMIN", "iat": int(time.time()) - 7200, "exp": int(time.time()) - 3600})))
check("Token bien firmado pero VENCIDO -> 401 TOKEN_EXPIRADO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_EXPIRADO", r.text)
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "sgad-backend", "sub": "fantasma", "rol": "ROLE_ADMIN", "exp": int(time.time()) + 600})))
check("Token válido de un usuario que NO existe -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO")
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "otro-sistema", "sub": "admin", "rol": "ROLE_ADMIN", "exp": int(time.time()) + 600})))
check("Token de otro emisor -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO")
r = requests.get(BASE + "/api/escuelas", headers=bearer(token_manual({"iss": "sgad-backend", "sub": "admin", "rol": "ROLE_ADMIN"})))
check("Token sin vencimiento (exp) -> TOKEN_INVALIDO", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO")
r = requests.get(BASE + "/api/escuelas", headers={"Authorization": "Basic YWRtaW46YWRtaW4xMjM="})
check("Otro esquema (Basic) no se acepta -> 401", r.status_code == 401)

# Usuario deshabilitado / cambio de rol en la BD: el token deja de servir / el rol vigente manda
print("── B3. Cambios en la base de datos ──")
try:
    import pymongo
    col = pymongo.MongoClient("mongodb://localhost:27017", serverSelectionTimeoutMS=3000)["sgad"]["usuarios"]
    s = requests.Session()
    rr = s.get(BASE + "/registro"); csrf = re.search(r'name="_csrf"\s+value="([^"]+)"', rr.text)
    rr = s.post(BASE + "/registro", data={"username": NUEVO, "password": "clave123", "confirmarPassword": "clave123", "rol": "ROLE_DEPORTISTA", "_csrf": csrf.group(1) if csrf else ""}, allow_redirects=False)
    _, tk_nuevo = obtener_token(NUEVO, "clave123")
    check("Usuario recién registrado obtiene su JWT", bool(tk_nuevo))
    r = requests.post(BASE + "/api/escuelas", json={"nombre": "X", "direccion": "Y", "telefono": "3001112233"}, headers=bearer(tk_nuevo))
    check("...y como DEPORTISTA no puede crear escuelas", r.status_code == 403)
    col.update_one({"username": NUEVO}, {"$set": {"rol": "ROLE_ADMIN"}})
    r = requests.post(BASE + "/api/escuelas", json={"nombre": "Via BD", "direccion": "Y", "telefono": "3001112233"}, headers=bearer(tk_nuevo))
    check("Si un admin le sube el rol en la BD, el MISMO token ya lo refleja (el rol vigente es el de la BD)", r.status_code == 201, r.text)
    if r.status_code == 201: requests.delete(f"{BASE}/api/escuelas/{j(r).get('idEscuela')}", headers=H)
    col.update_one({"username": NUEVO}, {"$set": {"habilitado": False}})
    r = requests.get(BASE + "/api/escuelas", headers=bearer(tk_nuevo))
    check("Usuario deshabilitado: su token aunque no haya vencido se rechaza", r.status_code == 401 and j(r).get("codigo") == "TOKEN_INVALIDO", r.text)
    r, _ = obtener_token(NUEVO, "clave123")
    check("...y tampoco puede pedir un token nuevo", r.status_code == 401)
    col.delete_one({"username": NUEVO})
except Exception as e:
    check("Pruebas con MongoDB (pymongo)", False, repr(e))

# ═════════ Convivencia sesión + JWT ═════════
print("── Convivencia: cookie + JWT ──")
ses, r = login_sesion("admin", "admin123")
check("Login con formulario sigue funcionando (cookie JSESSIONID)", r.status_code == 302 and "JSESSIONID" in ses.cookies.get_dict(), r.headers.get("Location"))
me = ses.get(BASE + "/api/auth/me")
check("Con la cookie, /me dice metodo SESION", j(me).get("metodo") == "SESION" and j(me).get("sessionTimeoutSeconds") == 60, me.text)
r = ses.get(BASE + "/api/auth/token")
tk_de_sesion = j(r).get("accessToken")
check("El token entregado a una sesión dura lo mismo que la sesión (expiresIn = 60 s en esta prueba)", j(r).get("expiresIn") == 60, r.text)
_pl = json.loads(b64d(tk_de_sesion.split(".")[1]))
check("...y su 'exp' en el contenido coincide", _pl["exp"] - _pl["iat"] == 60, _pl)
check("Quien ya tiene sesión puede cambiarla por un JWT (GET /api/auth/token)", r.status_code == 200 and tk_de_sesion and j(r).get("username") == "admin", r.text)
r = requests.get(BASE + "/api/escuelas", headers=bearer(tk_de_sesion))
check("...y ese JWT funciona SIN cookie", r.status_code == 200)
r = requests.get(BASE + "/api/auth/token")
check("GET /api/auth/token sin sesión -> 401", r.status_code == 401)
r = requests.get(BASE + "/api/auth/token", headers=bearer(tk_admin))
check("Un JWT NO sirve para pedir otro JWT (no se renueva solo) -> 403", r.status_code == 403, r.text)
r = requests.post(BASE + "/api/auth/token", json={"username": "admin", "password": "admin123"}, headers=bearer(tk_admin))
check("...ni mandando el login con un Bearer puesto -> 403", r.status_code == 403, r.text)
me = ses.get(BASE + "/api/auth/me", headers=bearer(tk_dep))
check("Si la petición trae cookie Y token, manda el token (metodo JWT, usuario deportista)", j(me).get("metodo") == "JWT" and j(me).get("username") == "deportista", me.text)
r = requests.get(BASE + "/panel", headers=bearer(tk_admin), allow_redirects=False)
check("Un Bearer en una página del backend (no /api) no abre nada -> redirige a /login", r.status_code == 302 and "/login" in r.headers.get("Location", ""), r.status_code)
ses.get(BASE + "/logout", allow_redirects=False)
check("Al cerrar sesión la cookie muere...", ses.get(BASE + "/api/escuelas").status_code == 401)
check("...pero el JWT sigue vivo hasta su vencimiento (así funciona JWT)", requests.get(BASE + "/api/escuelas", headers=bearer(tk_de_sesion)).status_code == 200)

print("── CORS (Angular en :4200 enviando Authorization) ──")
r = requests.options(BASE + "/api/escuelas", headers={**ORIGIN, "Access-Control-Request-Method": "GET", "Access-Control-Request-Headers": "authorization"})
check("Preflight con cabecera Authorization permitido", r.status_code == 200 and r.headers.get("Access-Control-Allow-Origin") == "http://localhost:4200" and "authorization" in r.headers.get("Access-Control-Allow-Headers", "").lower(), dict(r.headers))
r = requests.get(BASE + "/api/escuelas", headers={**ORIGIN, **bearer("basura")})
check("El 401 por token malo también lleva cabecera CORS (Angular lo puede leer)", r.status_code == 401 and r.headers.get("Access-Control-Allow-Origin") == "http://localhost:4200", dict(r.headers))

# ═════════ C. Google (OAuth2) ═════════
print("── C. Google / OAuth2 ──")
r = requests.get(BASE + "/login")
check("Sin credenciales de Google NO aparece el botón (la app sigue funcionando)", r.status_code == 200 and "Continuar con Google" not in r.text)
r = requests.get(BASE + "/oauth2/authorization/google", allow_redirects=False)
check("...y /oauth2/authorization/google no manda a Google", "accounts.google.com" not in r.headers.get("Location", ""), r.headers.get("Location"))

try:
    r = requests.get(BASE_GOOGLE + "/login", timeout=5)
    check("Con SGAD_GOOGLE_CLIENT_ID el login muestra 'Continuar con Google'", r.status_code == 200 and "Continuar con Google" in r.text and "/oauth2/authorization/google" in r.text)
    sg = requests.Session()
    r = sg.get(BASE_GOOGLE + "/oauth2/authorization/google", allow_redirects=False)
    loc = r.headers.get("Location", ""); u = urlparse(loc); q = parse_qs(u.query)
    check("El botón redirige a accounts.google.com", r.status_code == 302 and u.netloc == "accounts.google.com", loc)
    check("...con el client_id configurado", q.get("client_id") == ["test-client-id.apps.googleusercontent.com"], q)
    check("...flujo de código (response_type=code)", q.get("response_type") == ["code"])
    check("...pidiendo openid, profile y email", set(q.get("scope", [""])[0].split()) == {"openid", "profile", "email"}, q.get("scope"))
    check("...con redirect_uri de vuelta al SGAD", q.get("redirect_uri") == [BASE_GOOGLE + "/login/oauth2/code/google"], q.get("redirect_uri"))
    check("...con state y nonce anti-falsificación", bool(q.get("state")) and bool(q.get("nonce")), q)
    r = sg.get(BASE_GOOGLE + "/login/oauth2/code/google?code=falso&state=estado-que-no-es", allow_redirects=False)
    check("Un callback falso (state incorrecto) se rechaza -> /login?oauthError", r.status_code == 302 and "oauthError" in r.headers.get("Location", ""), r.headers.get("Location"))
    r = sg.get(BASE_GOOGLE + "/login?oauthError=true")
    check("...y se muestra el aviso 'No se pudo iniciar sesión con Google'", "No se pudo iniciar sesión con Google" in r.text)
    r = requests.get(BASE_GOOGLE + "/api/escuelas")
    check("El backend con Google también protege la API (401 sin credenciales)", r.status_code == 401)
except requests.exceptions.ConnectionError:
    check("Backend con Google en :8081 disponible", False, "no está levantado")

total = len(resultados); ok = sum(1 for _, p, _ in resultados if p)
print(f"\nRESULTADO: {ok}/{total} pruebas OK")
sys.exit(0 if ok == total else 1)
