import { Component } from '@angular/core';
import { NotificacionService } from '../../services/notificacion.service';

@Component({
  selector: 'app-toast',
  template: `
    <div class="toast-zona" aria-live="polite">
      <div *ngFor="let n of notificaciones.notificaciones$ | async"
           class="toast" [class.error]="n.tipo === 'error'" [class.aviso]="n.tipo === 'aviso'" [class.exito]="n.tipo === 'exito'">
        <span class="msg">{{ n.mensaje }}</span>
        <button class="cerrar" (click)="notificaciones.cerrar(n.id)" aria-label="Cerrar">×</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-zona {
      position: fixed; top: 72px; right: 20px; z-index: 2000;
      display: flex; flex-direction: column; gap: 10px; max-width: 380px;
    }
    .toast {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 12px 14px; border-radius: 8px; font-size: 14px; line-height: 1.35;
      background: #1c1c1c; color: #fff; border: 1px solid #333; border-left-width: 4px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.45);
    }
    .toast.error { border-left-color: #ef4444; }
    .toast.aviso { border-left-color: #f59e0b; }
    .toast.exito { border-left-color: #a3e635; }
    .msg { flex: 1; white-space: pre-line; }
    .cerrar { background: transparent; border: none; color: #aaa; font-size: 18px; cursor: pointer; line-height: 1; }
    .cerrar:hover { color: #fff; }
  `]
})
export class ToastComponent {
  constructor(public notificaciones: NotificacionService) {}
}
