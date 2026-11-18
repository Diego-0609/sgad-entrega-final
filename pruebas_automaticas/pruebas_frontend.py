import re, sys, time
from playwright.sync_api import sync_playwright

FRONT = "http://localhost:4200"
BACK = "http://localhost:8080"
SHOTS = "/home/claude/trabajo/tests/capturas"
import os; os.makedirs(SHOTS, exist_ok=True)
resultados = []

def check(nombre, cond, detalle=""):
    resultados.append((nombre, bool(cond), detalle))
    print(("PASS " if cond else "FAIL ") + nombre + ("" if cond else f"   -> {detalle}"), flush=True)

def entrar(browser, usuario, clave):
    ctx = browser.new_context(viewport={"width": 1280, "height": 760})
    page = ctx.new_page()
    page.on("dialog", lambda d: d.accept())
    respuestas = []
    page.on("response", lambda r: respuestas.append((r.status, r.url)))
    page.goto(FRONT + "/")
    page.wait_for_url(re.compile(r".*:8080/login.*"), timeout=15000)
    page.fill("#username", usuario)
    page.fill("#password", clave)
    page.click("button[type=submit]")
    page.wait_for_url(re.compile(r"http://localhost:4200/.*"), timeout=15000)
    page.wait_for_selector("app-navbar .navbar-user", timeout=15000)
    return ctx, page, respuestas

with sync_playwright() as pw:
    browser = pw.chromium.launch()

    # ───── DEPORTISTA ─────
    ctx, page, resp = entrar(browser, "deportista", "deportista123")
    check("Sin sesión, Angular manda al login del backend y tras entrar vuelve al dashboard", page.url.rstrip("/").endswith("/dashboard") or "dashboard" in page.url, page.url)
    nav = page.inner_text("app-navbar")
    check("Navbar muestra usuario y rol", "deportista" in nav and "DEPORTISTA" in nav, nav)
    check("Navbar muestra la cuenta regresiva de la sesión (mm:ss)", re.search(r"\d\d:\d\d", nav) is not None, nav)
    check("DEPORTISTA no ve Escuelas / Entrenadores / Materiales en el menú",
          all(x not in nav for x in ["Escuelas", "Entrenadores", "Materiales"]), nav)
    page.wait_for_timeout(1500)
    check("El dashboard del DEPORTISTA no genera peticiones 403", not [r for r in resp if r[0] == 403], [r for r in resp if r[0] == 403])
    page.screenshot(path=f"{SHOTS}/01_deportista_dashboard.png")
    page.goto(FRONT + "/entrenamientos"); page.wait_for_selector("h1")
    cuerpo = page.inner_text("app-root")
    check("DEPORTISTA ve Entrenamientos pero SIN botón 'Nuevo' ni columna Acciones", "Entrenamientos" in cuerpo and "+ Nuevo" not in cuerpo and "Acciones" not in cuerpo, cuerpo[:200])
    page.goto(FRONT + "/asistencias"); page.wait_for_selector("h1")
    cuerpo = page.inner_text("app-root")
    check("DEPORTISTA ve Asistencias SIN botón 'Registrar' ni Acciones", "+ Registrar" not in cuerpo and "Acciones" not in cuerpo, cuerpo[:200])
    page.goto(FRONT + "/escuelas"); page.wait_for_timeout(1200)
    check("DEPORTISTA entrando por URL a /escuelas es devuelto al dashboard (guard)", page.url.rstrip("/").endswith("/dashboard"), page.url)
    page.goto(FRONT + "/materiales"); page.wait_for_timeout(1200)
    check("...y a /materiales también", page.url.rstrip("/").endswith("/dashboard"), page.url)
    ctx.close()

    # ───── ADMIN ─────
    ctx, page, resp = entrar(browser, "admin", "admin123")
    nav = page.inner_text("app-navbar")
    check("ADMIN ve todos los módulos", all(x in nav for x in ["Escuelas", "Entrenadores", "Deportistas", "Entrenamientos", "Asistencias", "Materiales"]), nav)
    page.goto(FRONT + "/escuelas"); page.wait_for_selector("h1")
    page.click("text=+ Nueva Escuela")
    campos = page.locator(".modal input")
    campos.nth(0).fill("Escuela UI Test"); campos.nth(1).fill("Calle 123"); campos.nth(2).fill("3001234567")
    page.click(".modal button[type=submit]")
    page.wait_for_selector("td:has-text('Escuela UI Test')", timeout=8000)
    check("ADMIN crea una escuela desde la interfaz (CRUD frontend + backend)", True)

    # error de validación del backend mostrado en un toast
    page.goto(FRONT + "/deportistas"); page.wait_for_selector("h1")
    page.click("text=+ Nuevo Deportista")
    page.locator(".modal input").nth(0).fill("Prueba UI")
    page.locator(".modal input").nth(1).fill("0")
    page.locator(".modal select").nth(0).select_option("Masculino")
    page.locator(".modal input").nth(2).fill("Fútbol")
    page.locator(".modal select").nth(1).select_option(index=1)
    page.click(".modal button[type=submit]")
    page.wait_for_selector(".toast.error", timeout=8000)
    toast = page.inner_text(".toast.error")
    check("Error de validación del backend se muestra como aviso al usuario", "edad" in toast.lower(), toast)
    page.screenshot(path=f"{SHOTS}/02_admin_toast_error.png")
    page.click(".modal .btn-cancel")

    # limpiar la escuela creada
    page.goto(FRONT + "/escuelas"); page.wait_for_selector("td:has-text('Escuela UI Test')")
    fila = page.locator("tr:has-text('Escuela UI Test')")
    fila.locator(".btn-del").click()
    page.wait_for_timeout(1000)
    check("ADMIN elimina la escuela desde la interfaz", page.locator("td:has-text('Escuela UI Test')").count() == 0)

    # logout
    page.click("text=Cerrar sesión")
    page.wait_for_url(re.compile(r".*:8080/login.*logout.*"), timeout=10000)
    check("Cerrar sesión lleva al login con el mensaje de sesión cerrada", "Sesión cerrada correctamente" in page.inner_text("body"))
    page.goto(FRONT + "/dashboard")
    page.wait_for_url(re.compile(r".*:8080/login.*"), timeout=10000)
    check("Tras cerrar sesión, entrar a Angular vuelve a pedir login", True)
    ctx.close()

    # ───── EXPIRACIÓN DE SESIÓN EN LA INTERFAZ (timeout = 1 min) ─────
    ctx, page, resp = entrar(browser, "entrenador", "entrenador123")
    inicio = page.inner_text("app-navbar .session-timer")
    print("   cuenta regresiva inicial:", inicio, flush=True)
    check("ENTRENADOR ve Deportistas y Materiales, no Escuelas", "Deportistas" in page.inner_text("app-navbar") and "Materiales" in page.inner_text("app-navbar") and "Escuelas" not in page.inner_text("app-navbar"))
    page.wait_for_timeout(20000)
    despues = page.inner_text("app-navbar .session-timer")
    check("La cuenta regresiva baja con el tiempo", despues < inicio, f"{inicio} -> {despues}")
    page.screenshot(path=f"{SHOTS}/03_entrenador_contador.png")
    page.wait_for_url(re.compile(r".*:8080/login.*expired.*"), timeout=70000)
    check("Al llegar a 00:00 el usuario es enviado al login con aviso de sesión expirada",
          "expiró por inactividad" in page.inner_text("body"), page.url)
    page.screenshot(path=f"{SHOTS}/04_login_expirada.png")
    ctx.close()
    browser.close()

total = len(resultados); ok = sum(1 for _, p, _ in resultados if p)
print(f"\nRESULTADO FRONTEND: {ok}/{total} pruebas OK")
sys.exit(0 if ok == total else 1)
