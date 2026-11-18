import { Component, OnInit } from '@angular/core';
import { MiService } from '../../services/api.service';
import { Reporte } from '../../models/models';
import { formatearFechaHora } from '../../util/formato';

@Component({
  selector: 'app-buzon',
  template: `
    <div class="page">
      <div class="page-header"><h1>📬 Buzón de reportes</h1></div>

      <p class="vacio" *ngIf="cargado && reportes.length === 0">
        Todavía no tienes reportes. Cuando tu entrenador te envíe uno sobre tu rendimiento, aparecerá aquí.
      </p>

      <div class="reporte" *ngFor="let r of reportes" [class.nuevo]="!r.leido" (click)="abrir(r)">
        <div class="cabecera">
          <strong>{{ r.titulo }}</strong>
          <span class="badge" *ngIf="!r.leido">Nuevo</span>
          <span class="estrellas" *ngIf="r.calificacion">{{ '★'.repeat(r.calificacion) }}{{ '☆'.repeat(5 - r.calificacion) }}</span>
        </div>
        <span class="meta">De {{ r.autor }} · {{ fechaHora(r.fecha) }}</span>
        <p class="mensaje" *ngIf="abierto === r.idReporte">{{ r.mensaje }}</p>
        <span class="leer" *ngIf="abierto !== r.idReporte">Toca para leer</span>
      </div>
    </div>
  `,
  styles: [`
    .reporte { background: var(--bg-card); border: 1px solid var(--border-col); border-radius: 12px; padding: 16px 20px; margin-bottom: 12px; cursor: pointer; display: flex; flex-direction: column; gap: 4px; }
    .reporte.nuevo { border-left: 4px solid var(--accent); }
    .cabecera { display: flex; align-items: center; gap: 10px; }
    .estrellas { margin-left: auto; color: var(--accent); letter-spacing: 2px; }
    .meta, .leer, .vacio { font-size: 13px; color: var(--text-muted); }
    .mensaje { margin: 8px 0 0; white-space: pre-wrap; line-height: 1.5; }
  `]
})
export class BuzonComponent implements OnInit {
  reportes: Reporte[] = [];
  abierto?: string;
  cargado = false;

  constructor(private mi: MiService) {}

  ngOnInit() {
    this.mi.reportes().subscribe(lista => { this.reportes = lista; this.cargado = true; });
  }

  abrir(r: Reporte) {
    this.abierto = this.abierto === r.idReporte ? undefined : r.idReporte;
    if (this.abierto && !r.leido) {
      this.mi.marcarLeido(r.idReporte!).subscribe(() => r.leido = true);
    }
  }

  fechaHora = formatearFechaHora;
}
