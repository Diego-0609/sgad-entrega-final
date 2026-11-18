import { Injectable, Injector } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

/**
 * Agrega "Authorization: Bearer <token>" a las peticiones a la API.
 *
 * - /api/auth/me y /api/auth/token NO llevan el token: se resuelven con la cookie de sesión
 *   (así se sabe quién es el usuario y se le entrega/renueva el JWT).
 * - Antes de enviar, AuthService revisa que el token no esté por vencer; si lo está, lo renueva.
 * - Si por alguna razón no hay token, la petición sale solo con la cookie (como antes).
 *
 * Se usa Injector para evitar la dependencia circular HttpClient -> interceptores -> AuthService.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private injector: Injector) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const esApi = req.url.startsWith(environment.apiUrl);
    const esAuth = req.url.startsWith(`${environment.apiUrl}/auth/`);
    if (!environment.useJwt || !esApi || esAuth) {
      return next.handle(req);
    }
    return this.injector.get(AuthService).tokenVigente().pipe(
      switchMap(token => next.handle(
        token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req
      ))
    );
  }
}
