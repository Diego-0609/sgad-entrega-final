import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, finalize, interval, map, of, shareReplay, switchMap } from 'rxjs';
import { NotificacionService } from './notificacion.service';
import { environment } from '../../environments/environment';

const AUTH_ME_URL = `${environment.apiUrl}/auth/me`;
const AUTH_TOKEN_URL = `${environment.apiUrl}/auth/token`;
const LOGIN_URL = `${environment.backendUrl}/login`;
const LOGOUT_URL = `${environment.backendUrl}/logout`;

export interface UsuarioAutenticado {
  username: string;
  rol: string;
  sessionTimeoutSeconds?: number;
}

/**
 * Verifica contra el backend si hay una sesión de Spring Security activa
 * (cookie JSESSIONID, por eso las llamadas van con withCredentials true) y
 * guarda el rol del usuario, para que el resto de la app (navbar, guards)
 * decida qué mostrar y qué permitir.
 *
 * También lleva la cuenta regresiva de la sesión: el backend la cierra tras
 * N minutos SIN actividad (server.servlet.session.timeout), así que cada
 * respuesta del servidor reinicia el contador (registrarActividad) y, si
 * llega a cero, se manda al usuario al login con el aviso de "sesión expirada".
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private usuarioActual: UsuarioAutenticado | null = null;
  private motivoSinSesion: 'expired' | 'concurrent' | null = null;

  private timeoutSegundos = 0;
  private ultimaActividad = Date.now();
  private contadorIniciado = false;

  // JWT en memoria (no en localStorage): se pierde al recargar y se vuelve a pedir con la cookie de sesión
  private token: string | null = null;
  private tokenExpiraEn = 0;
  private tokenVidaMs = 0;
  private renovacion$: Observable<string | null> | null = null;

  /** Segundos que le quedan a la sesión (null si todavía no se sabe). */
  segundosRestantes: number | null = null;

  constructor(private http: HttpClient, private notificaciones: NotificacionService) {}

  verificarSesion(): Observable<boolean> {
    return this.http.get<UsuarioAutenticado>(AUTH_ME_URL, { withCredentials: true }).pipe(
      map(usuario => {
        this.usuarioActual = usuario;
        this.timeoutSegundos = usuario.sessionTimeoutSeconds ?? 0;
        this.registrarActividad();
        this.iniciarContador();
        return true;
      }),
      // Con la sesión confirmada, se cambia por un JWT (que usará el interceptor en cada petición)
      switchMap(ok => this.renovarToken().pipe(map(() => ok))),
      catchError((err: HttpErrorResponse) => {
        this.usuarioActual = null;
        this.motivoSinSesion = this.motivoDesdeCodigo(err?.error?.codigo);
        return of(false);
      })
    );
  }

  /**
   * Devuelve el JWT a usar. Si le queda menos de la mitad de su vida, lo renueva ANTES de que venza
   * cambiando la sesión (cookie) por uno nuevo; esa petición además mantiene viva la sesión.
   * Si el usuario estuvo inactivo y la sesión murió, la renovación recibe 401 y se va al login.
   */
  tokenVigente(): Observable<string | null> {
    if (!environment.useJwt) { return of(null); }
    const restante = this.tokenExpiraEn - Date.now();
    if (this.token && restante > this.tokenVidaMs / 2) { return of(this.token); }
    return this.renovarToken();
  }

  /** Pide un JWT con la cookie de sesión. Varias peticiones a la vez comparten UNA sola renovación. */
  private renovarToken(): Observable<string | null> {
    if (!environment.useJwt) { return of(null); }
    if (!this.renovacion$) {
      this.renovacion$ = this.http
        .get<{ accessToken: string; expiresIn: number }>(AUTH_TOKEN_URL, { withCredentials: true })
        .pipe(
          map(r => {
            this.token = r.accessToken;
            this.tokenVidaMs = r.expiresIn * 1000;
            this.tokenExpiraEn = Date.now() + this.tokenVidaMs;
            return this.token;
          }),
          catchError(() => { this.token = null; return of(null); }),   // el 401 ya lo atiende ErrorInterceptor
          finalize(() => { this.renovacion$ = null; }),
          shareReplay(1)
        );
    }
    return this.renovacion$;
  }

  /** Cada respuesta del servidor renueva el tiempo de vida de la sesión. */
  registrarActividad(): void {
    this.ultimaActividad = Date.now();
    this.actualizarRestante();
  }

  /** Lo llama el interceptor cuando el backend responde 401 con la sesión ya vencida. */
  sesionTerminada(codigo?: string): void {
    this.motivoSinSesion = this.motivoDesdeCodigo(codigo);
    this.usuarioActual = null;
    this.token = null;
    this.irALogin();
  }

  getUsuario(): UsuarioAutenticado | null {
    return this.usuarioActual;
  }

  esAdmin(): boolean {
    return this.usuarioActual?.rol === 'ROLE_ADMIN';
  }

  esEntrenador(): boolean {
    return this.usuarioActual?.rol === 'ROLE_ENTRENADOR';
  }

  esDeportista(): boolean {
    return this.usuarioActual?.rol === 'ROLE_DEPORTISTA';
  }

  // Admin y Entrenador son "personal" del sistema: gestionan Deportistas, Materiales,
  // Entrenamientos y Asistencias. El Deportista solo consulta.
  esPersonal(): boolean {
    return this.esAdmin() || this.esEntrenador();
  }

  irALogin(): void {
    const sufijo = this.motivoSinSesion ? `?${this.motivoSinSesion}=true` : '';
    window.location.href = LOGIN_URL + sufijo;
  }

  logout(): void {
    window.location.href = LOGOUT_URL;
  }

  // ── cuenta regresiva de la sesión ───────────────────────────────

  private iniciarContador(): void {
    if (this.contadorIniciado || this.timeoutSegundos <= 0) { return; }
    this.contadorIniciado = true;
    interval(1000).subscribe(() => {
      this.actualizarRestante();
      if (this.segundosRestantes === null || !this.usuarioActual) { return; }

      if (this.segundosRestantes === 60 && this.timeoutSegundos > 60) {
        this.notificaciones.aviso('Tu sesión expirará en 1 minuto por inactividad. Realiza cualquier acción para mantenerla activa.');
      }
      if (this.segundosRestantes <= 0) {
        this.sesionTerminada('SESION_EXPIRADA');
      }
    });
  }

  private actualizarRestante(): void {
    if (this.timeoutSegundos <= 0) { this.segundosRestantes = null; return; }
    const transcurridos = Math.floor((Date.now() - this.ultimaActividad) / 1000);
    this.segundosRestantes = Math.max(this.timeoutSegundos - transcurridos, 0);
  }

  private motivoDesdeCodigo(codigo?: string): 'expired' | 'concurrent' | null {
    if (codigo === 'SESION_EXPIRADA' || codigo === 'TOKEN_EXPIRADO') { return 'expired'; }
    if (codigo === 'SESION_REEMPLAZADA') { return 'concurrent'; }
    return null;
  }
}
