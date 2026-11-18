import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Hace que TODAS las llamadas HTTP del frontend incluyan la cookie de
 * sesión (withCredentials), sin tener que repetirlo en cada servicio.
 * Necesario porque el backend (localhost:8080) y el frontend
 * (localhost:4200) son orígenes distintos.
 */
@Injectable()
export class CredentialsInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const clonado = req.clone({ withCredentials: true });
    return next.handle(clonado);
  }
}
