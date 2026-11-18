import { Component, DoCheck } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { MiService } from '../../services/api.service';

@Component({
  selector: 'app-navbar',
  template: `
    <nav class="navbar">
      <div class="navbar-brand">
        <span class="logo">⚡</span>
        <span class="brand-text">SGAD</span>
      </div>
      <ul class="navbar-links">
        <ng-container *ngIf="esDeportista">
          <li><a routerLink="/mi-panel" routerLinkActive="active">Mi panel</a></li>
          <li><a routerLink="/mis-entrenamientos" routerLinkActive="active">Entrenamientos</a></li>
          <li><a routerLink="/mi-asistencia" routerLinkActive="active">Asistencia</a></li>
          <li><a routerLink="/mis-estadisticas" routerLinkActive="active">Estadísticas</a></li>
          <li>
            <a routerLink="/buzon" routerLinkActive="active">Buzón <span class="punto" *ngIf="noLeidos > 0">{{ noLeidos }}</span></a>
          </li>
        </ng-container>
        <ng-container *ngIf="!esDeportista">
          <li><a routerLink="/dashboard" routerLinkActive="active">Dashboard</a></li>
          <li *ngIf="esAdmin"><a routerLink="/escuelas" routerLinkActive="active">Escuelas</a></li>
          <li *ngIf="esAdmin"><a routerLink="/entrenadores" routerLinkActive="active">Entrenadores</a></li>
          <li *ngIf="esPersonal"><a routerLink="/deportistas" routerLinkActive="active">Deportistas</a></li>
          <li><a routerLink="/entrenamientos" routerLinkActive="active">Entrenamientos</a></li>
          <li><a routerLink="/asistencias" routerLinkActive="active">Asistencias</a></li>
          <li *ngIf="esPersonal"><a routerLink="/materiales" routerLinkActive="active">Materiales</a></li>
          <li *ngIf="esPersonal"><a routerLink="/reportes" routerLinkActive="active">Reportes</a></li>
        </ng-container>
      </ul>
      <div class="navbar-user" *ngIf="usuario">
        <span class="user-info">{{ usuario.username }} · {{ etiquetaRol }}</span>
        <span class="session-timer" *ngIf="tiempoRestante" [class.warn]="porExpirar" title="Tiempo restante de sesión por inactividad">⏱ {{ tiempoRestante }}</span>
        <button class="logout-btn" (click)="cerrarSesion()">Cerrar sesión</button>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      background: #000;
      border-bottom: 1px solid #222;
      padding: 0 32px;
      height: 60px;
    }
    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-right: 40px;
    }
    .logo {
      font-size: 20px;
      background: #a3e635;
      color: #111;
      width: 30px;
      height: 30px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .brand-text {
      color: #fff;
      font-weight: 800;
      font-size: 20px;
      letter-spacing: 2px;
    }
    .navbar-links {
      display: flex;
      list-style: none;
      gap: 4px;
      margin: 0;
      padding: 0;
      flex: 1;
    }
    .navbar-links a {
      color: #999;
      text-decoration: none;
      padding: 8px 14px;
      border-radius: 6px;
      font-size: 14px;
      transition: all 0.2s;
    }
    .navbar-links a:hover { color: #fff; background: #1a1a1a; }
    .navbar-links a.active {
      color: #111;
      background: #a3e635;
      font-weight: 700;
    }
    .punto {
      background: #a3e635;
      color: #111;
      border-radius: 10px;
      padding: 1px 7px;
      font-size: 11px;
      font-weight: 800;
      margin-left: 4px;
    }
    .navbar-user {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .user-info {
      color: #999;
      font-size: 13px;
    }
    .session-timer {
      color: #a3e635;
      font-size: 12px;
      font-variant-numeric: tabular-nums;
      border: 1px solid #2c2c2c;
      border-radius: 12px;
      padding: 3px 9px;
    }
    .session-timer.warn { color: #f59e0b; border-color: #f59e0b; }
    .logout-btn {
      background: transparent;
      border: 1px solid #444;
      color: #ccc;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .logout-btn:hover {
      border-color: #a3e635;
      color: #a3e635;
    }
  `]
})
export class NavbarComponent implements DoCheck {
  noLeidos = 0;
  private perfilPedido = false;

  constructor(private authService: AuthService, private mi: MiService) {
    this.mi.noLeidos$.subscribe(n => this.noLeidos = n);
  }

  /** Cuando se sabe que el usuario es deportista, se pide una vez su perfil para mostrar los reportes sin leer. */
  ngDoCheck(): void {
    if (this.esDeportista && !this.perfilPedido) {
      this.perfilPedido = true;
      this.mi.perfil().subscribe();
    }
  }

  get esDeportista(): boolean {
    return this.authService.esDeportista();
  }

  get usuario() {
    return this.authService.getUsuario();
  }

  get esAdmin(): boolean {
    return this.authService.esAdmin();
  }

  get esPersonal(): boolean {
    return this.authService.esPersonal();
  }

  get etiquetaRol(): string {
    const rol = this.usuario?.rol ?? '';
    return rol.replace('ROLE_', '');
  }

  /** mm:ss que le quedan a la sesión por inactividad */
  get tiempoRestante(): string {
    const total = this.authService.segundosRestantes;
    if (total === null) { return ''; }
    const m = Math.floor(total / 60).toString().padStart(2, '0');
    const sec = (total % 60).toString().padStart(2, '0');
    return `${m}:${sec}`;
  }

  get porExpirar(): boolean {
    const total = this.authService.segundosRestantes;
    return total !== null && total <= 60;
  }

  cerrarSesion(): void {
    this.authService.logout();
  }
}
