import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type TipoNotificacion = 'error' | 'aviso' | 'exito';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  mensaje: string;
}

/**
 * Mensajes emergentes (toasts) para avisar al usuario de errores, avisos
 * (ej. "tu sesión está por expirar") y confirmaciones, sin usar alert().
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private lista = new BehaviorSubject<Notificacion[]>([]);
  readonly notificaciones$ = this.lista.asObservable();
  private contador = 0;

  error(mensaje: string): void { this.agregar('error', mensaje, 7000); }
  aviso(mensaje: string): void { this.agregar('aviso', mensaje, 8000); }
  exito(mensaje: string): void { this.agregar('exito', mensaje, 4000); }

  cerrar(id: number): void {
    this.lista.next(this.lista.value.filter(n => n.id !== id));
  }

  private agregar(tipo: TipoNotificacion, mensaje: string, duracionMs: number): void {
    // Evita apilar el mismo mensaje repetido (ej. varias peticiones fallando a la vez)
    if (this.lista.value.some(n => n.mensaje === mensaje)) { return; }
    const id = ++this.contador;
    this.lista.next([...this.lista.value, { id, tipo, mensaje }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }
}
