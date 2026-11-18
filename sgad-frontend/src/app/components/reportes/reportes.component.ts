import { Component, OnInit } from '@angular/core';
import { DeportistaService, ReporteService } from '../../services/api.service';
import { NotificacionService } from '../../services/notificacion.service';
import { Deportista, Reporte } from '../../models/models';
import { formatearFechaHora } from '../../util/formato';

@Component({
  selector: 'app-reportes',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>📝 Reportes de rendimiento</h1>
        <button class="btn-primary" (click)="abrirModal()">+ Enviar reporte</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead><tr><th>Fecha</th><th>Deportista</th><th>Título</th><th>Nota</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr *ngFor="let r of reportes">
              <td>{{ fechaHora(r.fecha) }}</td>
              <td>{{ nombreDeportista(r.idDeportista) }}</td>
              <td>{{ r.titulo }}</td>
              <td>{{ r.calificacion ? r.calificacion + '/5' : '—' }}</td>
              <td><span class="badge">{{ r.leido ? 'Leído' : 'Sin leer' }}</span></td>
              <td class="acciones"><button class="btn-del" (click)="eliminar(r.idReporte!)">🗑️</button></td>
            </tr>
            <tr *ngIf="reportes.length === 0"><td colspan="6" class="empty">Todavía no has enviado reportes</td></tr>
          </tbody>
        </table>
      </div>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>Enviar reporte</h2>
          <form (ngSubmit)="enviar()">
            <label>Deportista</label>
            <select [(ngModel)]="form.idDeportista" name="idDeportista" required>
              <option value="">-- Selecciona deportista --</option>
              <option *ngFor="let d of deportistas" [value]="d.idDeportista">{{ d.nombre }}</option>
            </select>
            <label>Título</label>
            <input [(ngModel)]="form.titulo" name="titulo" required maxlength="100" placeholder="Ej: Avance de la semana">
            <label>Calificación (opcional)</label>
            <select [(ngModel)]="form.calificacion" name="calificacion">
              <option [ngValue]="null">Sin calificación</option>
              <option *ngFor="let n of [1,2,3,4,5]" [ngValue]="n">{{ n }} / 5</option>
            </select>
            <label>Mensaje</label>
            <textarea [(ngModel)]="form.mensaje" name="mensaje" required maxlength="1000" rows="5"
                      placeholder="¿Cómo le fue? ¿Qué debe mejorar?"></textarea>
            <div class="modal-actions">
              <button type="button" class="btn-cancel" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn-primary">Enviar</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    textarea { width: 100%; padding: 10px 14px; background: var(--bg-input); border: 1.5px solid var(--border-col); border-radius: 8px; font-size: 14px; font-family: 'Nunito', sans-serif; color: var(--text-main); outline: none; resize: vertical; }
    textarea:focus { border-color: var(--accent); }
  `]
})
export class ReportesComponent implements OnInit {
  reportes: Reporte[] = [];
  deportistas: Deportista[] = [];
  mostrarModal = false;
  form: Reporte = this.vacio();

  constructor(
    private svc: ReporteService,
    private deportistaSvc: DeportistaService,
    private avisos: NotificacionService
  ) {}

  ngOnInit() {
    this.cargar();
    this.deportistaSvc.listar().subscribe(d => this.deportistas = d);
  }

  cargar() { this.svc.listar().subscribe(d => this.reportes = d); }

  nombreDeportista(id: string): string {
    return this.deportistas.find(d => d.idDeportista === id)?.nombre ?? '—';
  }

  abrirModal() { this.form = this.vacio(); this.mostrarModal = true; }
  cerrarModal() { this.mostrarModal = false; }

  enviar() {
    this.svc.crear(this.form).subscribe(() => {
      this.avisos.exito('Reporte enviado al buzón del deportista.');
      this.cargar();
      this.cerrarModal();
    });
  }

  eliminar(id: string) {
    if (confirm('¿Eliminar este reporte?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  fechaHora = formatearFechaHora;

  private vacio(): Reporte {
    return { idDeportista: '', titulo: '', mensaje: '', calificacion: null };
  }
}
