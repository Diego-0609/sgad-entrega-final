# Regresión v2.2: peticiones con JWT + cookie JSESSIONID NO deben cambiar el id de la sesión.
import re, requests, concurrent.futures as cf
import sys
B = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"   # backend directo
s = requests.Session()
html = s.get(B + "/login").text
csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', html)
datos = {"username": "admin", "password": "admin123"}
if csrf: datos["_csrf"] = csrf.group(1)
s.post(B + "/login", data=datos, allow_redirects=False)
cookie0 = s.cookies.get("JSESSIONID")
tok = s.get(B + "/api/auth/token").json()["accessToken"]
print("Cookie de sesion tras login:", cookie0[:8], "| /api/auth/me:", s.get(B + "/api/auth/me").status_code)
def llamar(_):
    r = requests.get(B + "/api/escuelas", headers={"Authorization": "Bearer " + tok}, cookies={"JSESSIONID": cookie0})
    return r.status_code, r.headers.get("Set-Cookie", "")[:30]
with cf.ThreadPoolExecutor(5) as ex: res = list(ex.map(llamar, range(5)))
for r in res: print("  peticion con Bearer ->", r)
print("Con la cookie ORIGINAL, /api/auth/me ahora responde:", requests.get(B + "/api/auth/me", cookies={"JSESSIONID": cookie0}).status_code)

ok = requests.get(B + "/api/auth/me", cookies={"JSESSIONID": cookie0}).status_code == 200
print("RESULTADO:", "OK" if ok else "FALLA: el JWT está rotando el id de la sesión"); sys.exit(0 if ok else 1)
