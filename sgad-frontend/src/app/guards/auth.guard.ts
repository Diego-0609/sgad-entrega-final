import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Antes de dejar entrar a cualquier ruta de la app, pregunta al backend si
 * hay sesión activa. Si no la hay, redirige de inmediato al login del
 * backend (http://localhost:8080/login). Esto hace del login la puerta
 * única de entrada al sistema.
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService) {}

  canActivate(): Observable<boolean> {
    return this.authService.verificarSesion().pipe(
      tap(autenticado => {
        if (!autenticado) {
          this.authService.irALogin();
        }
      })
    );
  }
}
