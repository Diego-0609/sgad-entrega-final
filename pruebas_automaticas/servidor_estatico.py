# Sirve el build de Angular en :4200 con "fallback" a index.html (como hace `ng serve`)
import http.server, os, sys
RAIZ = "/home/claude/trabajo/ng-dist"
class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k): super().__init__(*a, directory=RAIZ, **k)
    def do_GET(self):
        ruta = self.translate_path(self.path.split("?")[0])
        if not os.path.exists(ruta) or os.path.isdir(ruta) and not os.path.exists(os.path.join(ruta, "index.html")):
            self.path = "/index.html"
        return super().do_GET()
    def log_message(self, *a): pass
http.server.ThreadingHTTPServer(("127.0.0.1", 4200), H).serve_forever()
