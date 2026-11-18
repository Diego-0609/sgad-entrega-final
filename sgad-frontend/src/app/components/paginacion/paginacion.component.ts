import { Component, Input } from '@angular/core';
import { Paginado } from '../../util/paginado';

@Component({
  selector: 'app-paginacion',
  template: `
    <div class="buscador" *ngIf="modo === 'buscador'">
      <input type="search" [placeholder]="placeholder" [ngModel]="pag.busqueda" (ngModelChange)="pag.buscar($event)">
      <span class="conteo">{{ pag.total }} resultados</span>
    </div>
    <div class="pie" *ngIf="modo === 'pie' && pag.paginas > 1">
      <button (click)="pag.ir(1)" [disabled]="pag.pagina === 1">«</button>
      <button (click)="pag.ir(pag.pagina - 1)" [disabled]="pag.pagina === 1">‹ Anterior</button>
      <span>Página {{ pag.pagina }} de {{ pag.paginas }}</span>
      <button (click)="pag.ir(pag.pagina + 1)" [disabled]="pag.pagina === pag.paginas">Siguiente ›</button>
      <button (click)="pag.ir(pag.paginas)" [disabled]="pag.pagina === pag.paginas">»</button>
    </div>
  `,
  styles: [`
    .buscador { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
    .buscador input {
      flex: 1; max-width: 360px; padding: 9px 12px; border-radius: 8px;
      background: var(--bg-input, #1a1a1a); color: var(--text-main, #fff); border: 1px solid var(--border-col, #333);
    }
    .conteo { color: var(--text-muted, #999); font-size: 13px; }
    .pie { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 16px; color: var(--text-muted, #999); font-size: 14px; }
    .pie button {
      padding: 6px 12px; border-radius: 6px; cursor: pointer;
      background: var(--bg-card, #151515); color: var(--text-main, #fff); border: 1px solid var(--border-col, #333);
    }
    .pie button:disabled { opacity: .4; cursor: default; }
  `]
})
export class PaginacionComponent {
  @Input() pag!: Paginado<any>;
  @Input() modo: 'buscador' | 'pie' = 'buscador';
  @Input() placeholder = 'Buscar...';
}
