import { Component, OnInit } from '@angular/core';
import { MiService } from '../../services/api.service';
import { EntrenamientoMio } from '../../models/models';
import { formatearFecha, formatearHora, yaPaso } from '../../util/formato';

@Component({
  selector: 'app-mis-entrenamientos',
  template: `
    <div class="page">
      <div class="page-header"><h1>📅 Mis entrenamientos</h1></div>

      <h2 class="seccion">Próximos</h2>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Fecha</th><th>Hora</th><th>Lugar</th><th>Entrenador</th></tr></thead>
          <tbody>
            <tr *ngFor="let t of proximos">
              <td>{{ fecha(t.fecha) }}</td><td>{{ hora(t.hora) }}</td><td>{{ t.lugar }}</td><td>{{ t.entrenador || '—' }}</td>
            </tr>
            <tr *ngIf="cargado && proximos.length === 0"><td colspan="4" class="empty">No tienes entrenamientos próximos</td></tr>
          </tbody>
        </table>
      </div>

      <h2 class="seccion">Anteriores</h2>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Fecha</th><th>Hora</th><th>Lugar</th><th>Entrenador</th><th>Mi asistencia</th></tr></thead>
          <tbody>
            <tr *ngFor="let t of anteriores">
              <td>{{ fecha(t.fecha) }}</td><td>{{ hora(t.hora) }}</td><td>{{ t.lugar }}</td><td>{{ t.entrenador || '—' }}</td>
              <td>
                <span class="badge" [class.ok]="t.asistencia === 'presente'" [class.mal]="t.asistencia === 'ausente'">
                  {{ t.asistencia || 'sin registrar' }}
                </span>
              </td>
            </tr>
            <tr *ngIf="cargado && anteriores.length === 0"><td colspan="5" class="empty">Todavía no hay entrenamientos anteriores</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .seccion { font-size: 17px; font-weight: 800; margin: 24px 0 10px; }
    .ok { background: #d4edda; color: #155724; }
    .mal { background: #f8d7da; color: #721c24; }
  `]
})
export class MisEntrenamientosComponent implements OnInit {
  proximos: EntrenamientoMio[] = [];
  anteriores: EntrenamientoMio[] = [];
  cargado = false;

  constructor(private mi: MiService) {}

  ngOnInit() {
    this.mi.entrenamientos().subscribe(lista => {
      this.proximos = lista.filter(t => !yaPaso(t.fecha, t.hora)).reverse();
      this.anteriores = lista.filter(t => yaPaso(t.fecha, t.hora));
      this.cargado = true;
    });
  }

  fecha = formatearFecha;
  hora = formatearHora;
}
