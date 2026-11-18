import { Component, OnInit } from '@angular/core';
import { MiService } from '../../services/api.service';
import { EntrenamientoMio } from '../../models/models';
import { formatearFecha, formatearHora } from '../../util/formato';

@Component({
  selector: 'app-mi-asistencia',
  template: `
    <div class="page">
      <div class="page-header"><h1>✅ Mi asistencia</h1></div>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Fecha</th><th>Hora</th><th>Lugar</th><th>Quién me entrena</th><th>Estado</th></tr></thead>
          <tbody>
            <tr *ngFor="let a of asistencias">
              <td>{{ fecha(a.fecha) }}</td><td>{{ hora(a.hora) }}</td><td>{{ a.lugar }}</td><td>{{ a.entrenador || '—' }}</td>
              <td><span class="badge" [class.ok]="a.asistencia === 'presente'" [class.mal]="a.asistencia === 'ausente'">{{ a.asistencia }}</span></td>
            </tr>
            <tr *ngIf="cargado && asistencias.length === 0"><td colspan="5" class="empty">Todavía no tienes asistencias registradas</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .ok { background: #d4edda; color: #155724; }
    .mal { background: #f8d7da; color: #721c24; }
  `]
})
export class MiAsistenciaComponent implements OnInit {
  asistencias: EntrenamientoMio[] = [];
  cargado = false;

  constructor(private mi: MiService) {}

  ngOnInit() {
    this.mi.asistencias().subscribe(lista => { this.asistencias = lista; this.cargado = true; });
  }

  fecha = formatearFecha;
  hora = formatearHora;
}
