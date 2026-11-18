import { Component, OnInit } from '@angular/core';
import { MiService } from '../../services/api.service';
import { EstadisticasMias } from '../../models/models';
import { formatearFecha } from '../../util/formato';

@Component({
  selector: 'app-mis-estadisticas',
  template: `
    <div class="page" *ngIf="est">
      <div class="page-header"><h1>📊 Mis estadísticas</h1></div>

      <div class="fila">
        <div class="anillo" [style.background]="'conic-gradient(var(--accent) ' + est.resumen.porcentaje + '%, #2a2a2a 0)'">
          <div class="centro"><strong>{{ est.resumen.porcentaje }}%</strong><span>asistencia</span></div>
        </div>
        <div class="tiles">
          <div class="tile"><strong>{{ est.resumen.total }}</strong><span>Entrenamientos</span></div>
          <div class="tile"><strong>{{ est.resumen.presentes }}</strong><span>Presente</span></div>
          <div class="tile"><strong>{{ est.resumen.ausentes }}</strong><span>Ausente</span></div>
          <div class="tile"><strong>{{ est.resumen.racha }}</strong><span>Racha seguida</span></div>
        </div>
      </div>

      <h2 class="seccion">Esta semana</h2>
      <p class="detalle">
        {{ fecha(est.resumen.semanaActual.desde) }} al {{ fecha(est.resumen.semanaActual.hasta) }}:
        <strong>{{ est.resumen.semanaActual.presentes }}</strong> presente(s) y
        <strong>{{ est.resumen.semanaActual.ausentes }}</strong> ausente(s)
        <ng-container *ngIf="est.resumen.semanaActual.total > 0">· {{ est.resumen.semanaActual.porcentaje }}% de asistencia</ng-container>
        <ng-container *ngIf="est.resumen.semanaActual.total === 0">· sin entrenamientos registrados</ng-container>
      </p>

      <h2 class="seccion">Últimas 4 semanas</h2>
      <div class="barras">
        <div class="barra" *ngFor="let s of est.resumen.ultimasSemanas">
          <span class="valor">{{ s.porcentaje }}%</span>
          <div class="columna"><div class="relleno" [style.height.%]="s.porcentaje"></div></div>
          <span class="semana">{{ s.desde | slice:5 }}</span>
          <span class="conteo">{{ s.presentes }}/{{ s.total }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .fila { display: flex; gap: 32px; align-items: center; flex-wrap: wrap; }
    .anillo { width: 160px; height: 160px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
    .centro { width: 120px; height: 120px; border-radius: 50%; background: var(--bg-card); display: flex; flex-direction: column; align-items: center; justify-content: center; }
    .centro strong { font-size: 30px; font-weight: 800; }
    .centro span { font-size: 12px; color: var(--text-muted); }
    .tiles { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; flex: 1; min-width: 280px; }
    .tile { background: var(--bg-card); border: 1px solid var(--border-col); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 2px; }
    .tile strong { font-size: 26px; font-weight: 800; }
    .tile span, .detalle { font-size: 13px; color: var(--text-muted); }
    .seccion { font-size: 17px; font-weight: 800; margin: 28px 0 10px; }
    .barras { display: flex; gap: 20px; align-items: flex-end; }
    .barra { display: flex; flex-direction: column; align-items: center; gap: 4px; width: 70px; }
    .columna { width: 36px; height: 120px; background: #2a2a2a; border-radius: 6px; display: flex; align-items: flex-end; overflow: hidden; }
    .relleno { width: 100%; background: var(--accent); min-height: 2px; }
    .valor { font-size: 12px; font-weight: 700; }
    .semana, .conteo { font-size: 11px; color: var(--text-muted); }
  `]
})
export class MisEstadisticasComponent implements OnInit {
  est?: EstadisticasMias;

  constructor(private mi: MiService) {}

  ngOnInit() {
    this.mi.estadisticas().subscribe(e => this.est = e);
  }

  fecha = formatearFecha;
}
