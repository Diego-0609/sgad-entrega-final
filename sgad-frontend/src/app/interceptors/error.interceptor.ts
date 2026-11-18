import { Injectable, Injector } from '@angular/core';
import {
  HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse
} from '@angular/common/http';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';
import { environment } from '../../environments/environment';

const API_URL = environment.apiUrl;

/**
 * Control de errores centralizado del frontend. Todas las respuestas de la
 * API pasan por aquí:
 *
 *  - respuesta correcta  -> se renueva el contador de la sesión
 *  - 401 (sin sesión / sesión expirada / sesión reemplazada) -> al login
 *  - 403 (rol sin permiso)  -> aviso "No tienes permisos..."
 *  - 400 / 404 / 5xx        -> se muestra el mensaje (y el detalle por campo)
 *                              que manda el backend en su JSON de error
 *  - sin conexión (status 0) -> aviso de que no hay comunicación con el servidor
 *
 * Se usa Injector (y no inyección directa) para evitar la dependencia circular
 * HttpClient -> interceptores -> AuthService -> HttpClient.
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private injector: Injector) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const esApi = req.url.startsWith(API_URL);
    return next.handle(req).pipe(
      tap(evento => {
        if (esApi && evento instanceof HttpResponse) {
          this.injector.get(AuthService).registrarActividad();
        }
      }),
      catchError((err: HttpErrorResponse) => {
        if (esApi) { this.manejar(req, err); }
        return throwError(() => err);
      })
    );
  }

  private manejar(req: HttpRequest<unknown>, err: HttpErrorResponse): void {
    const auth = this.injector.get(AuthService);
    const avisos = this.injector.get(NotificacionService);
    const cuerpo = err.error && typeof err.error === 'object' ? err.error : {};

    if (err.status === 0) {
      avisos.error('No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.');
      return;
    }

    if (err.status === 401) {
      // /api/auth/me lo resuelve el AuthGuard (redirige al login por su cuenta)
      if (!req.url.endsWith('/api/auth/me')) {
        auth.sesionTerminada(cuerpo.codigo);
      }
      return;
    }

    if (err.status === 403) {
      avisos.error(cuerpo.mensaje ?? 'No tienes permisos para realizar esta acción.');
      return;
    }

    let mensaje: string = cuerpo.mensaje ?? this.mensajePorDefecto(err.status);
    if (cuerpo.errores && typeof cuerpo.errores === 'object') {
      const detalle = Object.values(cuerpo.errores as Record<string, string>).join('\n');
      if (detalle) { mensaje = detalle; }
    }
    avisos.error(mensaje);
  }

  private mensajePorDefecto(status: number): string {
    if (status === 404) { return 'El recurso solicitado no existe.'; }
    if (status >= 500) { return 'Ocurrió un error en el servidor. Intenta de nuevo en unos momentos.'; }
    return 'No se pudo completar la operación.';
  }
}
