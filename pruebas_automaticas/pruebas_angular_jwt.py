"""
Prueba en navegador real: Angular usa el JWT.
Requiere: backend en :8080 con SGAD_SESSION_TIMEOUT=1m, y el Angular compilado servido en :4200 (servidor_estatico.py).
El token que recibe una sesión dura lo mismo que la sesión (60 s aquí), así que se puede probar la renovación en ~40 s.
"""
import re, sys, time, json, base64
from playwright.sync_api import sync_playwright

FRONT, BACK = "http://localhost:4200", "http://localhost:8080"
res = []
def check(n, c, d=""):
    res.append(bool(c)); print(("PASS " if c else "FAIL ") + n + ("" if c else f"   -> {d}"), flush=True)
def payload(t): return json.loads(base64.urlsafe_b64decode(t.split(".")[1] + "=" * (-len(t.split(".")[1]) % 4)))

with sync_playwright() as p:
    br = p.chromium.launch(); page = br.new_page(); peticiones = []
    page.on("request", lambda r: peticiones.append((time.time(), r.method, r.url, r.headers.get("authorization"))) if "/api/" in r.url else None)

    page.goto(FRONT)
    page.wait_for_url(re.compile(r".*:8080/login.*"), timeout=15000)
    page.click("[data-demo-user=admin]"); page.click("button[type=submit]")
    page.wait_for_url(re.compile(r"http://localhost:4200/.*"), timeout=15000)
    page.wait_for_selector("text=Escuelas", timeout=15000); page.wait_for_timeout(1500)

    me = [x for x in peticiones if x[2].endswith("/api/auth/me")]
    tok = [x for x in peticiones if x[2].endswith("/api/auth/token")]
    datos = [x for x in peticiones if "/api/" in x[2] and "/api/auth/" not in x[2] and x[1] == "GET"]
    check("Angular pide /api/auth/me con la COOKIE (sin Bearer)", me and all(x[3] is None for x in me), me)
    check("Con la sesión confirmada, pide su JWT a /api/auth/token (cookie, sin Bearer)", len(tok) >= 1 and all(x[3] is None for x in tok), tok)
    check("Las peticiones de datos (dashboard) salen con 'Authorization: Bearer ...'", datos and all((x[3] or "").startswith("Bearer ") for x in datos), datos)
    t1 = datos[0][3].split(" ", 1)[1]
    pl = payload(t1)
    check("El token es de 'admin', ROLE_ADMIN y dura lo mismo que la sesión (60 s)", pl["sub"] == "admin" and pl["rol"] == "ROLE_ADMIN" and pl["exp"] - pl["iat"] == 60, pl)

    # El Bearer manda sobre la cookie: un token malo falla aunque la cookie de sesión sea válida
    r = page.evaluate("""async (u) => { const r = await fetch(u + '/api/escuelas', {credentials:'include', headers:{Authorization:'Bearer token.falso.xx'}}); return [r.status, (await r.json()).codigo]; }""", BACK)
    check("Con cookie válida pero Bearer falso -> 401 TOKEN_INVALIDO (manda el token)", r == [401, "TOKEN_INVALIDO"], r)

    # Renovación: a los >30 s ya le queda menos de la mitad al token -> se cambia por uno nuevo usando la cookie
    antes = len(peticiones)
    page.wait_for_timeout(34000)
    page.click("a:has-text('Deportistas')"); page.wait_for_timeout(2500)
    nuevas = peticiones[antes:]
    tok2 = [x for x in nuevas if x[2].endswith("/api/auth/token")]
    datos2 = [x for x in nuevas if "/api/" in x[2] and "/api/auth/" not in x[2] and x[1] == "GET"]
    check("Tras >30 s Angular renueva el token ANTES de que venza (nuevo GET /api/auth/token con cookie)", len(tok2) == 1 and tok2[0][3] is None, tok2)
    t2 = datos2[0][3].split(" ", 1)[1] if datos2 else ""
    check("...y las peticiones siguientes usan el token NUEVO", t2 and t2 != t1 and payload(t2)["iat"] > pl["iat"], (t1[-12:], t2[-12:]))
    check("La pantalla sigue funcionando sin pedir login (Deportistas cargó)", "/deportistas" in page.url and "login" not in page.url, page.url)

    # Logout limpia todo
    page.click("text=Cerrar sesión") if page.locator("text=Cerrar sesión").count() else page.goto(BACK + "/logout")
    page.wait_for_url(re.compile(r".*:8080/login.*logout.*"), timeout=10000)
    check("Cerrar sesión lleva al login", True)
    br.close()

print(f"\nRESULTADO ANGULAR+JWT: {sum(res)}/{len(res)} pruebas OK"); sys.exit(0 if all(res) else 1)
