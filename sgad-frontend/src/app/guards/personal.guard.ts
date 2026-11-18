import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Restringe rutas del personal (Deportistas, Materiales) para que el rol
 * DEPORTISTA no las vea ni las gestione. Solo ADMIN y ENTRENADOR pasan.
 */
@Injectable({ providedIn: 'root' })
export class PersonalGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.authService.esPersonal()) {
      return true;
    }
    this.router.navigate(['/dashboard']);
    return false;
  }
}
