import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Restringe rutas exclusivas del administrador (Escuelas, Entrenadores).
 * Si un entrenador intenta entrar por URL directa, lo manda al dashboard
 * en vez de mostrarle esa sección.
 */
@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.authService.esAdmin()) {
      return true;
    }
    this.router.navigate(['/dashboard']);
    return false;
  }
}
