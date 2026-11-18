import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Solo el rol DEPORTISTA entra a su panel personal; los demás vuelven al dashboard. */
@Injectable({ providedIn: 'root' })
export class DeportistaGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    return this.auth.esDeportista() || this.router.createUrlTree(['/dashboard']);
  }
}

/** El dashboard es de ADMIN y ENTRENADOR: el deportista va directo a su propio panel. */
@Injectable({ providedIn: 'root' })
export class InicioGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    return !this.auth.esDeportista() || this.router.createUrlTree(['/mi-panel']);
  }
}
