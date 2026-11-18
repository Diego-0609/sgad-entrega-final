import { Component, OnInit } from '@angular/core';
import { MiService } from '../../services/api.service';
import { EstadisticasMias, PerfilMio } from '../../models/models';
import { formatearFecha, formatearHora } from '../../util/formato';

@Component({
  selector: 'app-mi-panel',
  template: `
    <div class="page" *ngIf="perfil">
      <h1 class="titulo">Hola, {{ perfil.deportista.nombre }} 👋</h1>
      <p class="sub">{{ perfil.escuela ? perfil.escuela : 'Todavía no estás asignado a una escuela' }}</p>

      <div class="aviso" *ngIf="!perfil.escuela">
        Tu entrenador o el administrador debe asignarte a una escuela para que aquí aparezcan tus entrenamientos.
      </div>

      <div class="cards">
        <a class="card" routerLink="/mis-entrenamientos">
          <span class="etiqueta">Próximo entrenamiento</span>
          <ng-container *ngIf="est?.proximo as p; else sinProximo">
            <span class="grande">{{ fecha(p.fecha) }}</span>
            <span class="detalle">{{ hora(p.hora) }} · {{ p.lugar }}</span>
            <span class="detalle">Entrena: {{ p.entrenador || 'por asignar' }}</span>
          </ng-container>
          <ng-template #sinProximo><span class="detalle">No hay entrenamientos programados</span></ng-template>
        </a>

        <a class="card" routerLink="/mis-estadisticas">
          <span class="etiqueta">Mi asistencia</span>
          <span class="grande">{{ est?.resumen?.porcentaje ?? 0 }}%</span>
          <span class="detalle">{{ est?.resumen?.presentes ?? 0 }} de {{ est?.resumen?.total ?? 0 }} entrenamientos</span>
        </a>

        <a class="card" routerLink="/mis-estadisticas">
          <span class="etiqueta">Esta semana</span>
          <span class="grande">{{ est?.resumen?.semanaActual?.presentes ?? 0 }}/{{ est?.resumen?.semanaActual?.total ?? 0 }}</span>
          <span class="detalle">Racha: {{ est?.resumen?.racha ?? 0 }} seguidas</span>
        </a>

        <a class="card" [class.nuevo]="perfil.reportesNoLeidos > 0" routerLink="/buzon">
          <span class="etiqueta">Buzón de reportes</span>
          <span class="grande">{{ perfil.reportesNoLeidos }}</span>
          <span class="detalle">{{ perfil.reportesNoLeidos === 1 ? 'reporte sin leer' : 'reportes sin leer' }}</span>
        </a>
      </div>

      <h2 class="seccion">Mis entrenadores</h2>
      <div class="entrenadores" *ngIf="perfil.entrenadores.length; else sinEntrenadores">
        <div class="entrenador" *ngFor="let e of perfil.entrenadores">
          <strong>{{ e.nombre }}</strong>
          <span>{{ e.especialidad }}</span>
          <span class="tel">{{ e.telefono }}</span>
        </div>
      </div>
      <ng-template #sinEntrenadores><p class="detalle">Aún no hay entrenadores en tu escuela.</p></ng-template>
    </div>
  `,
  styles: [`
    .titulo { font-size: 28px; font-weight: 800; margin: 0; }
    .sub { color: var(--text-muted); margin: 4px 0 24px; }
    .aviso { background: #2a2108; border: 1px solid #5c4a12; color: #f5d57a; padding: 12px 16px; border-radius: 10px; margin-bottom: 20px; font-size: 14px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 16px; }
    .card { background: var(--bg-card); border: 1px solid var(--border-col); border-left: 4px solid var(--accent); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 6px; text-decoration: none; color: inherit; transition: transform .2s, border-color .2s; }
    .card:hover { transform: translateY(-3px); border-color: var(--accent); }
    .card.nuevo { box-shadow: 0 0 0 1px var(--accent); }
    .etiqueta { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: .5px; }
    .grande { font-size: 26px; font-weight: 800; color: var(--text-main); line-height: 1.2; }
    .detalle { font-size: 13px; color: var(--text-muted); }
    .seccion { font-size: 18px; font-weight: 800; margin: 32px 0 12px; }
    .entrenadores { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 12px; }
    .entrenador { background: var(--bg-card); border: 1px solid var(--border-col); border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; font-size: 14px; }
    .entrenador span { color: var(--text-muted); font-size: 13px; }
    .tel { color: var(--accent) !important; }
  `]
})
export class MiPanelComponent implements OnInit {
  perfil?: PerfilMio;
  est?: EstadisticasMias;

  constructor(private mi: MiService) {}

  ngOnInit() {
    this.mi.perfil().subscribe(p => this.perfil = p);
    this.mi.estadisticas().subscribe(e => this.est = e);
  }

  fecha = formatearFecha;
  hora = formatearHora;
}
